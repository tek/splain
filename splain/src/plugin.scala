package splain

import tools.nsc._
import collection.mutable.{Buffer, Map}

trait Formatting
{ self: Analyzer =>
  import global._
  import Console._

  def featureInfix: Boolean

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

  def isSymbolic(tpe: Type) =
    tpe.typeSymbol.name.encodedName.toString !=
      tpe.typeSymbol.name.decodedName.toString

  def formatRefinement(sym: Symbol) = {
    if (sym.hasRawInfo) {
      val rhs = formatInfix(sym.rawInfo, true)
      s"$sym = $rhs"
    }
    else sym.toString
  }

  def formatSimpleType(tpe: Type) = tpe match {
    case a: RefinedType =>
      val simple = a.parents.map(formatInfix(_, true)).mkString(" with ")
      val refine = a.decls.map(formatRefinement).mkString("; ")
      s"$simple {$refine}"
    case a =>
      a.typeSymbol.name.decodedName.toString
  }

  def formatTypeApply(simple: String, args: List[String]) =
    args.mkString(s"$simple[", ",", "]")

  def formatTuple(args: List[String]) =
    args match {
      case head :: Nil => head
      case _ => args.mkString("(", ",", ")")
    }

  def formatFunction(args: List[String]) = {
    val (params, returnt) = args.splitAt(args.length - 1)
    s"${formatTuple(params)} => ${formatTuple(returnt)}"
  }

  def wrapParens(expr: String, top: Boolean) =
    if (top) expr else s"($expr)"

  def formatType[A](tpe: Type, args: List[A], top: Boolean,
    rec: A => Boolean => String): String = {
    val simple = formatSimpleType(tpe)
    def formattedArgs = args.map(rec(_)(true))
    if (simple.startsWith("Function"))
      wrapParens(formatFunction(formattedArgs), top)
    else if (simple.startsWith("Tuple")) formatTuple(formattedArgs)
    else args match {
      case left :: right :: Nil if isSymbolic(tpe) =>
        val l = rec(left)(false)
        val r = rec(right)(false)
        val t = s"$l $simple $r"
        wrapParens(t, top)
      case head :: tail =>
        formatTypeApply(simple, formattedArgs)
      case _ =>
        simple
    }
  }

  def formatInfix(tpe: Type, top: Boolean): String = {
    val rec = (tp: Type) => (t: Boolean) => formatInfix(tp, t)
    if (featureInfix) formatType(tpe, tpe.typeArgs, top, rec)
    else tpe.toLongString
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

  val candidateRegex = """.*\.this\.(.*)""".r

  def formatCandidate(what: Any) =
    what.toString match {
      case candidateRegex(suf) => suf
      case a => a
    }

  def formatNestedImplicit(err: ImpError): List[String] = {
    val candidate = formatCandidate(err.what)
    val tpe = dealias(err.tpe)
    val problem =
      s"$RED$candidate$RESET invalid for $GREEN${tpe}$RESET because"
    val hasMatching = "hasMatchingSymbol reported error: "
    List(problem, err.reason.stripPrefix(hasMatching))
  }

  def formatImplicitParam(sym: Symbol) = sym.name

  def formatImplicitMessage(param: Symbol, hasExtra: Boolean,
    extra: Seq[String]) = {
      val paramName = formatImplicitParam(param)
      val ptp = dealias(param.tpe)
      val nl = if (extra.isEmpty) "" else "\n"
      val ex = extra.mkString("\n")
      val pre = if (hasExtra) "implicit error;\n" else ""
      s"$pre$RED!${BLUE}I$RESET $YELLOW$paramName$RESET: $ptp$nl$ex"
    }
}

trait Implicits
extends typechecker.Implicits
with typechecker.ContextErrors
with Formatting
{ self: Analyzer =>
  import global._

  def featureImplicits: Boolean

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

  def inferImplicit2(tree: Tree, pt: Type, reportAmbiguous: Boolean,
    isView: Boolean, context: Context, saveAmbiguousDivergent: Boolean,
    pos: Position)
  : SearchResult = {
    import typechecker.ImplicitsStats._
    import reflect.internal.util.Statistics
    if (implicitNesting == 0) implicitErrors.clear()
    implicitNesting += 1
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

  override def inferImplicit(tree: Tree, pt: Type, r: Boolean, v: Boolean,
    context: Context, s: Boolean, pos: Position): SearchResult = {
      if (featureImplicits) inferImplicit2(tree, pt, r, v, context, s, pos)
      else super.inferImplicit(tree, pt, r, v, context, s, pos)
  }

  class ImplicitSearch2(tree: Tree, pt: Type, isView: Boolean,
    context0: Context, pos0: Position = NoPosition)
  extends ImplicitSearch(tree, pt, isView, context0, pos0)
  {
    override def failure(what: Any, reason: String, pos: Position = this.pos)
    : SearchResult = {
      ImpError(pt, what, reason) +=: implicitErrors
      super.failure(what, reason, pos)
    }
  }

  def noImplicitError(tree: Tree, param: Symbol)
  (implicit context: Context): Unit = {
    def errMsg = {
      val hasExtra = implicitNesting == 0 && !implicitErrors.isEmpty
      val extra =
        if (hasExtra) implicitErrors.distinct flatMap formatNestedImplicit
        else Nil
      val symbol = param.tpe.typeSymbolDirect
      symbol match {
        case ImplicitNotFoundMsg(msg) =>
          def typeArgsAtSym(ptp: Type) = ptp.baseType(symbol).typeArgs
          msg.format(typeArgsAtSym(param.tpe).map(formatInfix(_, true)))
        case _ =>
          formatImplicitMessage(param, hasExtra, extra)
      }
    }
    ErrorUtils.issueNormalTypeError(tree, errMsg)
  }

  override def NoImplicitFoundError(tree: Tree, param: Symbol)
  (implicit context: Context): Unit = {
    if (featureImplicits) noImplicitError(tree, param)
    else super.NoImplicitFoundError(tree, param)
  }
}

trait TypeDiagnostics
extends typechecker.TypeDiagnostics
with Formatting
{ self: Analyzer =>
  import global._

  def featureFoundReq: Boolean

  def foundReqMsgShort(found: Type, req: Type): String =
    formatDiff(found, req, true)

  def foundReqMsgNormal(found: Type, req: Type): String = {
    withDisambiguation(Nil, found, req)(formatDiff(found, req, true)) +
    explainVariance(found, req) +
    explainAnyVsAnyRef(found, req)
  }

  override def foundReqMsg(found: Type, req: Type): String =
    if (featureFoundReq) ";\n  " + foundReqMsgShort(found, req)
    else super.foundReqMsg(found, req)
}

trait Analyzer
extends typechecker.Analyzer
with Implicits
with TypeDiagnostics

class SplainPlugin(val global: Global)
extends plugins.Plugin
{ plugin =>
  val analyzer =
    new { val global = plugin.global } with Analyzer {
      def featureImplicits = boolean(keyImplicits)
      def featureFoundReq = boolean(keyFoundReq)
      def featureInfix = boolean(keyInfix)
    }

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

  override def processOptions(options: List[String], error: String => Unit) = {
    def invalid(opt: String) = error(s"splain: invalid option `$opt`")
    def setopt(key: String, value: String) = {
      if (opts.contains(key)) opts.update(key, value)
      else invalid(key)
    }
    options foreach { opt =>
      opt.split(":").toList match {
        case key :: value :: Nil => setopt(key, value)
        case key :: Nil => setopt(key, "true")
        case _ => invalid(opt)
      }
    }
  }

  val name = "splain"
  val description = "better types and implicit errors"
  val components = Nil

  val keyImplicits = "implicits"
  val keyFoundReq = "foundreq"
  val keyInfix = "infix"

  val opts: Map[String, String] = Map(
    keyImplicits -> "true",
    keyFoundReq -> "true",
    keyInfix -> "true"
  )

  def opt(key: String, default: String) = opts.getOrElse(key, default)

  def boolean(key: String) = opt(key, "true") == "true"
}
