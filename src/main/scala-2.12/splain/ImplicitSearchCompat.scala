package splain

import scala.tools.nsc._

trait ImplicitSearchCompat
extends ImplicitSearchBounds
{ self: Analyzer with ImplicitChains with typechecker.ContextErrors =>
  import global._

  class ImplicitSearchSplain(tree: Tree, pt: Type, isView: Boolean, context0: Context, pos0: Position = NoPosition)
  extends ImplicitSearch(tree, pt, isView, context0, pos0)
  with Bounds
  {
    override val infer = new InferencerImpl {
      def context = ImplicitSearchSplain.this.context

      override def isCoercible(tp: Type, pt: Type) =
        undoLog undo viewExists(tp, pt)
    }
  }
}
