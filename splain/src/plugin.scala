package tryp

import tools.nsc._, plugins.Plugin
import reflect.internal.util.Statistics
import collection.mutable.Buffer

trait Formatting
{ self: Analyzer =>
  import global._

  object DealiasedType
  extends TypeMap
  {
    def apply(tp: Type): Type = tp match {
      case TypeRef(pre, sym, args)
      if sym.isAliasType && !sym.isInDefaultNamespace =>
        mapOver(tp.dealias)
      case _ =>
        mapOver(tp)
    }
  }

  def dealias(tpe: Type) = formatInfix(DealiasedType(tpe), true)

  def isSymbolic(sym: Symbol) =
    sym.name.encodedName.toString != sym.name.decodedName.toString

  def printSym(sym: Symbol) = sym.name.decodedName.toString

  def formatTypeApply(sym: String, args: List[String]) =
    args.mkString(s"$sym[", ",", "]")

  def formatType[A](tpe: Type, args: List[A], top: Boolean,
    rec: A => Boolean => String): String = {
    val sym = printSym(tpe.typeSymbol)
    args match {
      case left :: right :: Nil if isSymbolic(tpe.typeSymbol) =>
        val l = rec(left)(false)
        val r = rec(right)(false)
        val t = s"$l $sym $r"
        if (top) t else s"($t)"
      case a @ (head :: tail) =>
        formatTypeApply(sym, a.map(rec(_)(true)))
      case _ =>
        sym
    }
  }

  def formatInfix(tpe: Type, top: Boolean): String = {
    val rec = (tp: Type) => (t: Boolean) => formatInfix(tp, t)
    formatType(tpe, tpe.typeArgs, top, rec)
  }

  def formatDiff(found: Type, req: Type, top: Boolean): String = {
    import Console._
    if (found.typeSymbol == req.typeSymbol) {
      val rec = (l: Type, r: Type) => (t: Boolean) => formatDiff(l, r, t)
      val recT = rec.tupled
      val args = found.typeArgs zip req.typeArgs
      formatType(found, args, top, recT)
    }
    else {
      val l = formatInfix(found, true)
      val r = formatInfix(req, true)
      s"$RED$l$RESET|$GREEN$r$RESET"
    }
  }
}

trait Implicits
extends typechecker.Implicits
with Formatting
{ self: Analyzer =>
  import global._
  import Console._

  case class ImpError(tpe: Type, what: Any, reason: String)
  {
    override def equals(other: Any) = other match {
      case ImpError(t, _, _) => t == tpe
      case _ => false
    }

    override def hashCode = tpe.hashCode
  }

  var implicitNesting = 0
  val implicitErrors = Buffer[ImpError]()

  override def inferImplicit(tree: Tree, pt: Type, reportAmbiguous: Boolean,
    isView: Boolean, context: Context, saveAmbiguousDivergent: Boolean,
    pos: Position): SearchResult = {
      if (implicitNesting == 0) implicitErrors.clear()
      implicitNesting += 1
      import typechecker.ImplicitsStats._
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
    if (shouldPrint)
      typingStack.printTyping(tree, "typing implicit: %s %s"
        .format(tree, context.undetparamsString))
    val implicitSearchContext = context.makeImplicit(reportAmbiguous)
    val result =
      new ImplicitSearch2(tree, pt, isView, implicitSearchContext, pos)
        .bestImplicit
    if (result.isFailure && saveAmbiguousDivergent &&
      implicitSearchContext.reporter.hasErrors)
      implicitSearchContext.reporter
        .propagateImplicitTypeErrorsTo(context.reporter)
    context.undetparams =
      ((context.undetparams ++ result.undetparams)
        .filterNot(result.subst.from.contains)).distinct
    if (Statistics.canEnable) Statistics.stopTimer(implicitNanos, start)
    if (Statistics.canEnable) Statistics.stopCounter(rawTypeImpl, rawTypeStart)
    if (Statistics.canEnable)
      Statistics.stopCounter(findMemberImpl, findMemberStart)
    if (Statistics.canEnable) Statistics.stopCounter(subtypeImpl, subtypeStart)

    implicitNesting -= 1
    result
  }

  class ImplicitSearch2(tree: Tree, pt: Type, isView: Boolean,
    context0: Context, pos0: Position = NoPosition)
  extends super.ImplicitSearch(tree, pt, isView, context0, pos0)
  {
    override def failure(what: Any, reason: String, pos: Position = this.pos)
    : SearchResult = {
      implicitErrors += (ImpError(pt, what, reason))
      super.failure(what, reason, pos)
    }
  }

  def formatNestedImplicit(err: ImpError)
  : List[String] = {
    val tpe = dealias(err.tpe)
    val problem =
      s"$RED${err.what}$RESET invalid for $GREEN${tpe}$RESET because"
    val hasMatching = "hasMatchingSymbol reported error: "
    List(problem, err.reason.stripPrefix(hasMatching))
  }

  override def NoImplicitFoundError(tree: Tree, param: Symbol)
  (implicit context: Context): Unit = {
    def errMsg = {
      val extra =
        if (implicitNesting == 0 && !implicitErrors.isEmpty)
          implicitErrors.distinct flatMap formatNestedImplicit
        else Nil
      val paramName = param.name
      val paramTp = param.tpe
      val symbol = paramTp.typeSymbolDirect
      symbol match {
        case ImplicitNotFoundMsg(msg) =>
          def typeArgsAtSym(ptp: Type) = ptp.baseType(symbol).typeArgs
          msg.format(typeArgsAtSym(paramTp).map(formatInfix(_, true)))
        case _ =>
          val ptp = dealias(paramTp)
          val nl = if (extra.isEmpty) "" else "\n"
          val ex = extra.mkString("\n")
          s"${RED}!${BLUE}I$RESET $YELLOW$paramName$RESET: $ptp$nl$ex"
      }
    }
    ErrorUtils.issueNormalTypeError(tree, errMsg)
  }
}

trait TypeDiagnostics
extends typechecker.TypeDiagnostics
with Formatting
{ self: Analyzer =>
  import global._

  def foundReqMsgShort(found: Type, req: Type): String =
    formatDiff(found, req, true)

  def foundReqMsgNormal(found: Type, req: Type): String = {
    withDisambiguation(Nil, found, req)(formatDiff(found, req, true)) +
    explainVariance(found, req) +
    explainAnyVsAnyRef(found, req)
  }

  override def foundReqMsg(found: Type, req: Type): String =
    ";\n  " + foundReqMsgShort(found, req)
}

trait Analyzer
extends typechecker.Analyzer
with Implicits
with TypeDiagnostics

class SplainPlugin(val global: Global)
extends Plugin
{ plugin =>
  val analyzer = new { val global = plugin.global } with Analyzer

  val analyzerField = classOf[Global].getDeclaredField("analyzer")
  analyzerField.setAccessible(true)
  analyzerField.set(global, analyzer)

  val phasesSetMapGetter = classOf[Global]
    .getDeclaredMethod("phasesSet")
  val phasesSet = phasesSetMapGetter
    .invoke(global)
    .asInstanceOf[scala.collection.mutable.Set[SubComponent]]
  if (phasesSet.exists(_.phaseName == "typer")) {
  def subcomponentNamed(name: String) =
    phasesSet
      .find(_.phaseName == name)
      .head
    val oldScs @ List(oldNamer, oldPackageobjects, oldTyper) =
      List(subcomponentNamed("namer"),
        subcomponentNamed("packageobjects"),
        subcomponentNamed("typer"))
    val newScs = List(analyzer.namerFactory,
      analyzer.packageObjects,
      analyzer.typerFactory)
    phasesSet --= oldScs
    phasesSet ++= newScs
  }

  val name = "splain"
  val description = "better types and implicit errors"
  val components = Nil
}
