package splain

import tools.nsc._

import StringColor._

trait ImplicitChains
extends typechecker.Implicits
with typechecker.ContextErrors
with Formatting
{ self: Analyzer =>
  import global._

  def featureImplicits: Boolean
  def featureBounds: Boolean

  val candidateRegex = """.*\.this\.(.*)""".r

  trait ImpFailReason
  {
    def tpe: Type
    def candidate: Tree

    lazy val unapplyCandidate = candidate match {
      case TypeApply(name, _) => name
      case a => a
    }

    def candidateName = unapplyCandidate match {
      case Select(_, name) => name.toString
      case Ident(name) => name.toString
      case a => a.toString
    }

    lazy val cleanCandidate = {
      unapplyCandidate.toString match {
        case candidateRegex(suf) => suf
        case a => a
      }
    }

    override def equals(other: Any) = other match {
      case o: ImpFailReason =>
        o.tpe == tpe && candidateName == o.candidateName
      case _ => false
    }

    override def hashCode = tpe.hashCode
  }

  case class ImpError(tpe: Type, candidate: Tree, param: Symbol)
  extends ImpFailReason

  case class NonConfBounds
  (tpe: Type, candidate: Tree, targs: List[Type], tparams: List[Symbol])
  extends ImpFailReason

  var implicitTypeStack = List[Type]()
  var implicitErrors = List[ImpFailReason]()

  def nestedImplicit = implicitTypeStack.nonEmpty

  def removeErrorsFor(tpe: Type) =
    implicitErrors = implicitErrors.dropWhile(_.tpe == tpe)

  def search
  (tree: Tree, pt: Type, isView: Boolean, context: Context, pos: Position) = {
    val resultType = Option(tree.tpe).map(_.resultType)
    val repeat =
      resultType.exists(OptionOps.contains(_)(
        implicitErrors.headOption.map(_.tpe)))
    if (!nestedImplicit) implicitErrors = List()
    implicitTypeStack = pt :: implicitTypeStack
    val result =
      new ImplicitSearchCompat(tree, pt, isView, context, pos)
        .bestImplicit
    if (result.isSuccess) removeErrorsFor(pt)
    implicitTypeStack = implicitTypeStack.drop(1)
    result
  }

  def inferImplicitImpl(tree: Tree, pt: Type, reportAmbiguous: Boolean,
    isView: Boolean, context: Context, saveAmbiguousDivergent: Boolean,
    pos: Position)
  : SearchResult = {
    import typechecker.ImplicitsStats._
    import reflect.internal.util.Statistics
    val shouldPrint = printTypings && !context.undetparams.isEmpty
    val rawTypeStart =
      if (Statistics.canEnable) Statistics.startCounter(rawTypeImpl) else null
    val findMemberStart =
      if (Statistics.canEnable) Statistics.startCounter(findMemberImpl)
      else null
    val subtypeStart =
      if (Statistics.canEnable) Statistics.startCounter(subtypeImpl) else null
    val start =
      if (Statistics.canEnable) Statistics.startTimer(implicitNanos)
      else null
    val implicitSearchContext = context.makeImplicit(reportAmbiguous)
    inferImplicitPre(shouldPrint, tree, pt, isView, context)
    val result = search(tree, pt, isView, implicitSearchContext, pos)
    inferImplicitPost(result, saveAmbiguousDivergent, context,
      implicitSearchContext)
    if (Statistics.canEnable) Statistics.stopTimer(implicitNanos, start)
    if (Statistics.canEnable) Statistics.stopCounter(rawTypeImpl, rawTypeStart)
    if (Statistics.canEnable)
      Statistics.stopCounter(findMemberImpl, findMemberStart)
    if (Statistics.canEnable) Statistics.stopCounter(subtypeImpl, subtypeStart)
    result
  }

  override def inferImplicit(tree: Tree, pt: Type, r: Boolean, v: Boolean,
    context: Context, s: Boolean, pos: Position): SearchResult = {
      if (featureImplicits) inferImplicitImpl(tree, pt, r, v, context, s, pos)
      else super.inferImplicit(tree, pt, r, v, context, s, pos)
  }

  abstract class ImplicitSearchImpl(tree: Tree, pt: Type, isView: Boolean,
    context0: Context, pos0: Position = NoPosition)
  extends ImplicitSearch(tree, pt, isView, context0, pos0)
  {
    trait InferencerImpl
    { self: Inferencer =>
      import InferErrorGen._

      /**
       * Duplication of the original method, because the error is created
       * within.
       */
      override def checkBounds(tree: Tree, pre: Type, owner: Symbol,
        tparams: List[Symbol], targs: List[Type], prefix: String): Boolean = {
        def issueBoundsError() = {
          notWithinBounds(tree, prefix, targs, tparams, Nil)
          false
        }
        def issueKindBoundErrors(errs: List[String]) = {
          KindBoundErrors(tree, prefix, targs, tparams, errs)
          false
        }
        def check() = checkKindBounds(tparams, targs, pre, owner) match {
          case Nil  =>
            isWithinBounds(pre, owner, tparams, targs) || issueBoundsError()
          case errs =>
            (targs contains WildcardType) || issueKindBoundErrors(errs)
        }
        targs.exists(_.isErroneous) || tparams.exists(_.isErroneous) || check()
      }

      def notWithinBounds(tree: Tree, prefix: String, targs: List[Type],
        tparams: List[Symbol], kindErrors: List[String]) = {
          val err = NonConfBounds(pt, tree, targs, tparams)
          implicitErrors = err :: implicitErrors
          NotWithinBounds(tree, prefix, targs, tparams, Nil)
      }
    }
  }

  def noImplicitError(tree: Tree, param: Symbol)
  (implicit context: Context): Unit = {
    if (!nestedImplicit) {
      val err =
        showStats("implicits", formatImplicitError(param, implicitErrors))
      ErrorUtils.issueNormalTypeError(tree, err)
    }
    else {
      implicitTypeStack
        .headOption
        .map(ImpError(_, tree, param))
        .foreach(err => implicitErrors = err :: implicitErrors)
      nativeNoImplicitFoundError(tree, param)
    }
  }

  def nativeNoImplicitFoundError(tree: Tree, param: Symbol)
  (implicit context: Context): Unit
}
