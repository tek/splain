package splain

trait ImplicitSearchBounds { self: Analyzer =>
  import global._

  trait Bounds { search: ImplicitSearch =>
    trait InferencerImpl extends Inferencer {
      import InferErrorGen._

      /** Duplication of the original method, because the error is created within.
        * `NotWithinBounds` cannot simply be overridden since it is a method in `Analyzer` and `pt` is needed for
        * `NonConfBounds`
        */
      def checkBoundsSplain(
        tree: Tree,
        pre: Type,
        owner: Symbol,
        tparams: List[Symbol],
        targs: List[Type],
        prefix: String,
      ): Boolean = {
        def issueBoundsError() = {
          notWithinBounds(tree, prefix, targs, tparams, Nil)
          false
        }
        def issueKindBoundErrors(errs: List[String]) = {
          KindBoundErrors(tree, prefix, targs, tparams, errs)
          false
        }
        def check() =
          checkKindBounds(tparams, targs, pre, owner) match {
            case Nil =>
              isWithinBounds(pre, owner, tparams, targs) || issueBoundsError()
            case errs =>
              (targs contains WildcardType) || issueKindBoundErrors(errs)
          }
        targs.exists(_.isErroneous) || tparams.exists(_.isErroneous) || check()
      }

      override def checkBounds(
        tree: Tree,
        pre: Type,
        owner: Symbol,
        tparams: List[Symbol],
        targs: List[Type],
        prefix: String,
      ): Boolean =
        if (featureBounds)
          checkBoundsSplain(tree, pre, owner, tparams, targs, prefix)
        else
          super.checkBounds(tree, pre, owner, tparams, targs, prefix)

      def notWithinBounds(
        tree: Tree,
        prefix: String,
        targs: List[Type],
        tparams: List[Symbol],
        kindErrors: List[String],
      ): Unit = {
        if (tree.tpe != null && tree.tpe =:= pt) {
          val err = NonConfBounds(pt, tree, implicitNesting, targs, tparams)
          implicitErrors = err :: implicitErrors
        }
        NotWithinBounds(tree, prefix, targs, tparams, kindErrors)
      }
    }
  }
}
