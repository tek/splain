package splain

import tools.nsc._

object OptionOps
{
  def contains[A](a: A)(o: Option[A]): Boolean = o.contains(a)
}

trait Compat
{ self: Analyzer =>
  import global._

  object TermNameCompat
  {
    def apply(n: String) = TermName(n)
  }
}

trait ImplicitsCompat
extends ImplicitChains
{ self: Analyzer =>
  import global._

  def inferImplicitPre(shouldPrint: Boolean, tree: Tree, pt: Type,
    isView: Boolean, context: Context) = {
    if (shouldPrint)
      typingStack.printTyping(tree, "typing implicit: %s %s"
        .format(tree, context.undetparamsString))
  }

  def inferImplicitPost(result: SearchResult, saveAmbiguousDivergent: Boolean,
    context: Context, implicitSearchContext: Context) = {
    if (result.isFailure && saveAmbiguousDivergent &&
      implicitSearchContext.reporter.hasErrors)
      implicitSearchContext.reporter
        .propagateImplicitTypeErrorsTo(context.reporter)
    context.undetparams =
      ((context.undetparams ++ result.undetparams)
        .filterNot(result.subst.from.contains)).distinct
  }

  class ImplicitSearchCompat(tree: Tree, pt: Type, isView: Boolean,
    context0: Context, pos0: Position = NoPosition)
  extends ImplicitSearchImpl(tree, pt, isView, context0, pos0)
  {
    override val infer =
      if (featureBounds) new InferencerCompat with InferencerImpl
      else new InferencerCompat

    class InferencerCompat
    extends Inferencer
    {
      def context = ImplicitSearchCompat.this.context

      override def isCoercible(tp: Type, pt: Type) =
        undoLog undo viewExists(tp, pt)
    }
  }

  override def NoImplicitFoundError(tree: Tree, param: Symbol)
  (implicit context: Context): Unit = {
    if (featureImplicits) noImplicitError(tree, param)
    else super.NoImplicitFoundError(tree, param)
  }
}
