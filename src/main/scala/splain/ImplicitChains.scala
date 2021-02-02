package splain

import splain.format.Formatting

import scala.tools.nsc._

trait ImplicitChains
extends typechecker.Implicits
with typechecker.ContextErrors
with Formatting
with ImplicitStats
with ImplicitsCompat
with WarningCompat { self: Analyzer =>
  import global._

  def featureImplicits: Boolean
  def featureBounds: Boolean

  val candidateRegex = """.*\.this\.(.*)""".r

  def shortName(ident: String) = ident.split('.').toList.lastOption.getOrElse(ident)

  trait ImpFailReason {
    def tpe: Type
    def candidate: Tree
    def nesting: Int

    lazy val unapplyCandidate =
      candidate match {
        case TypeApply(name, _) =>
          name
        case a =>
          a
      }

    def candidateName =
      unapplyCandidate match {
        case Select(_, name) =>
          name.toString
        case Ident(name) =>
          name.toString
        case a =>
          a.toString
      }

    lazy val cleanCandidate = {
      unapplyCandidate.toString match {
        case candidateRegex(suf) =>
          suf
        case a =>
          a
      }
    }

    override def equals(other: Any) =
      other match {
        case o: ImpFailReason =>
          o.tpe.toString == tpe.toString && candidateName == o.candidateName
        case _ =>
          false
      }

    override def hashCode = (tpe.toString.hashCode, candidateName.hashCode).hashCode
  }

  case class ImpError(tpe: Type, candidate: Tree, nesting: Int, param: Symbol) extends ImpFailReason {
    override def toString = s"ImpError(${shortName(tpe.toString)}, ${shortName(candidate.toString)}), $nesting, $param"
  }

  case class NonConfBounds(tpe: Type, candidate: Tree, nesting: Int, targs: List[Type], tparams: List[Symbol])
  extends ImpFailReason {
    override def toString =
      s"NonConfBounds(${shortName(tpe.toString)}, ${shortName(candidate.toString)}), $nesting, $targs, $tparams"
  }

  var implicitTypeStack = List[Type]()
  var implicitErrors = List[ImpFailReason]()
  def implicitNesting = implicitTypeStack.length - 1

  def nestedImplicit = implicitTypeStack.nonEmpty

  def removeErrorsFor(tpe: Type): Unit = implicitErrors = implicitErrors.dropWhile(_.tpe == tpe)

  def inferImplicitPre(shouldPrint: Boolean, tree: Tree, context: Context) =
    if (shouldPrint)
      typingStack.printTyping(tree, "typing implicit: %s %s".format(tree, context.undetparamsString))

  def inferImplicitPost(
    result: SearchResult,
    saveAmbiguousDivergent: Boolean,
    context: Context,
    implicitSearchContext: Context,
  ) = {
    if (result.isFailure && saveAmbiguousDivergent && implicitSearchContext.reporter.hasErrors)
      implicitSearchContext
        .reporter
        .propagateImplicitTypeErrorsTo(context.reporter)
    context.undetparams =
      (context.undetparams ++ result.undetparams)
        .filterNot(result.subst.from.contains)
        .distinct
    if (result.isSuccess && settings.warnSelfImplicit && result.tree.symbol != null) {
      val s =
        if (result.tree.symbol.isAccessor)
          result.tree.symbol.accessed
        else if (result.tree.symbol.isModule)
          result.tree.symbol.moduleClass
        else
          result.tree.symbol
      if (context.owner.hasTransOwner(s))
        contextWarning(context, result.tree.pos, s"Implicit resolves to enclosing ${result.tree.symbol}")
    }
    emitResult(implicitSearchContext)(result)
  }

  def inferImplicitImpl(
    tree: Tree,
    pt: Type,
    reportAmbiguous: Boolean,
    isView: Boolean,
    context: Context,
    saveAmbiguousDivergent: Boolean,
    pos: Position,
  ): SearchResult = {
    val shouldPrint = printTypings && !context.undetparams.isEmpty
    withImplicitStats { () =>
      val implicitSearchContext = context.makeImplicit(reportAmbiguous)
      inferImplicitPre(shouldPrint, tree, context)
      val result = search(tree, pt, isView, implicitSearchContext, pos)
      inferImplicitPost(result, saveAmbiguousDivergent, context, implicitSearchContext)
      result
    }
  }

  override def inferImplicit(
    tree: Tree,
    pt: Type,
    r: Boolean,
    v: Boolean,
    context: Context,
    s: Boolean,
    pos: Position,
  ): SearchResult =
    if (featureImplicits)
      inferImplicitImpl(tree, pt, r, v, context, s, pos)
    else
      super.inferImplicit(tree, pt, r, v, context, s, pos)

  def noImplicitError(tree: Tree, param: Symbol)(implicit context: Context): Unit =
    if (!nestedImplicit)
      scala.util.Try(showStats("implicits", formatImplicitError(param, implicitErrors))) match {
        case scala.util.Success(err) =>
          ErrorUtils.issueNormalTypeError(tree, err)
        case scala.util.Failure(exc) =>
          ErrorUtils.issueNormalTypeError(tree, s"fatal error in splain: $exc")
          nativeNoImplicitFoundError(tree, param)
      }
    else {
      implicitTypeStack
        .headOption
        .map(ImpError(_, tree, implicitNesting, param))
        .foreach(err => implicitErrors = err :: implicitErrors)
      nativeNoImplicitFoundError(tree, param)
    }

  override def NoImplicitFoundError(tree: Tree, param: Symbol)(implicit context: Context): Unit =
    if (featureImplicits)
      noImplicitError(tree, param)
    else
      super.NoImplicitFoundError(tree, param)

  def nativeNoImplicitFoundError(tree: Tree, param: Symbol)(implicit context: Context): Unit =
    super.NoImplicitFoundError(tree, param)
}
