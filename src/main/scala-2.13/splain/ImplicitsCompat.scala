package splain

trait ImplicitsCompat
extends ImplicitSearchCompat
{ self: Analyzer =>
  import global._
  import definitions.dropByName

  def search(tree: Tree, pt: Type, isView: Boolean, context: Context, pos: Position) = {
    if (!nestedImplicit) implicitErrors = List()
    implicitTypeStack = pt :: implicitTypeStack
    val dpt = if (isView) pt else dropByName(pt)
    val isByName = dpt ne pt
    val search = new ImplicitSearchSplain(tree, dpt, isView, context, pos, isByName)
    pluginsNotifyImplicitSearch(search)
    val result = search.bestImplicit
    pluginsNotifyImplicitSearchResult(result)
    if (result.isSuccess) removeErrorsFor(pt)
    implicitTypeStack = implicitTypeStack.drop(1)
    result
  }

  def emitResult(implicitSearchContext: Context)(result: SearchResult): SearchResult =
    implicitSearchContext.emitImplicitDictionary(result)
}
