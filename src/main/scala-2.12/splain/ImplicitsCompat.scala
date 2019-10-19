package splain

trait ImplicitsCompat
extends ImplicitSearchCompat
{ self: Analyzer with ImplicitChains =>
  import global._

  def search(tree: Tree, pt: Type, isView: Boolean, context: Context, pos: Position): SearchResult = {
    if (!nestedImplicit) implicitErrors = List()
    implicitTypeStack = pt :: implicitTypeStack
    val search = new ImplicitSearchSplain(tree, pt, isView, context, pos)
    pluginsNotifyImplicitSearch(search)
    val result = search.bestImplicit
    pluginsNotifyImplicitSearchResult(result)
    if (result.isSuccess) removeErrorsFor(pt)
    implicitTypeStack = implicitTypeStack.drop(1)
    result
  }

  def emitResult(implicitSearchContext: Context)(result: SearchResult): SearchResult =
    implicitSearchContext match { case _ => result }
}
