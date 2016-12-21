package splain

import tools.nsc._
import collection.mutable

object Messages
{
  val hasMatching = "hasMatchingSymbol reported error: "

  val typingTypeApply =
    "typing TypeApply reported errors for the implicit tree: "
}

object StringColor
{
  implicit class StringColorOps(s: String)
  {
    import Console._
    def red = RED + s + RESET
    def green = GREEN + s + RESET
    def yellow = YELLOW + s + RESET
    def blue = BLUE + s + RESET
  }
}
import StringColor._

trait Formatting
{ self: Analyzer =>
  import global._

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

  def dealias(tpe: Type) =
    if (isAux(tpe)) formatAux(tpe)
    else formatInfix(DealiasedType(tpe), true)

  def isSymbolic(tpe: Type) =
    tpe.typeSymbol.name.encodedName.toString !=
      tpe.typeSymbol.name.decodedName.toString

  def isAux(tpe: Type) =
    tpe.typeConstructor.toString.split('.').lastOption.contains("Aux")

  def formatAux(tpe: Type) = {
    val ctorNames = tpe.typeConstructor.toString.split('.')
    val ctor = ctorNames.takeRight(2).mkString(".")
    val args = bracket(tpe.typeArgs.map(formatInfix(_, true)))
    s"$ctor$args"
  }

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
      val name = a.typeSymbol.name.decodedName.toString
      if (a.typeSymbol.isModuleClass) s"$name.type"
      else name
  }

  def formatTypeApply(simple: String, args: List[String]) =
    args.mkString(s"$simple[", ",", "]")

  def formatTuple(args: List[String]) =
    args match {
      case head :: Nil => head
      case _ => args.mkString("(", ",", ")")
    }

  def bracket[A](params: List[A]) = params.mkString("[", ", ", "]")

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
    if (found.typeSymbol == req.typeSymbol) {
      val rec = (l: Type, r: Type) => (t: Boolean) => formatDiff(l, r, t)
      val recT = rec.tupled
      val args = found.typeArgs zip req.typeArgs
      formatType(found, args, top, recT)
    }
    else {
      val l = formatInfix(found, true)
      val r = formatInfix(req, true)
      s"${l.red}|${r.green}"
    }
  }

  def formatNestedImplicit(err: ImpError): List[String] = {
    val candidate = err.cleanCandidate
    val tpe = dealias(err.tpe)
    val extraInfo =
      if (tpe == dealias(err.prev)) ""
      else s" as ${tpe.green}"
    val problem = s"${candidate.red} invalid$extraInfo because"
    List(problem, err.cleanReason)
  }

  def formatNestedImplicits(errors: List[ImpError]) = {
    errors.distinct flatMap formatNestedImplicit
  }

  def formatImplicitParam(sym: Symbol) = sym.name.toString

  def formatImplicitMessage
  (param: Symbol, showStack: Boolean, errors: List[ImpError]) = {
    def stack = formatNestedImplicits(errors)
    val tpe = param.tpe
    val paramName = formatImplicitParam(param)
    val ptp = dealias(tpe)
    val nl = if (showStack && errors.nonEmpty) "\n" else ""
    val ex = if(showStack) stack.mkString("\n") else ""
    val pre = if (showStack) "implicit error;\n" else ""
    val bang = "!"
    val i = "I"
    s"${pre}${bang.red}${i.blue} ${paramName.yellow}: ${ptp.green}$nl$ex"
  }
}

trait Implicits
extends typechecker.Implicits
with typechecker.ContextErrors
with Formatting
{ self: Analyzer =>
  import global._

  def featureImplicits: Boolean
  def featureBounds: Boolean

  val candidateRegex = """.*\.this\.(.*)""".r

  case class ImpError(tpe: Type, candidate: Any, reason: String, prev: Type)
  {
    def candidateName = candidate match {
      case Select(_, name) => name
      case Ident(name) => name
      case a => a
    }

    lazy val cleanCandidate =
      candidate.toString match {
        case candidateRegex(suf) => suf
        case a => a
      }

    override def equals(other: Any) = other match {
      case o @ ImpError(t, _, _, _) =>
        t == tpe && candidateName == o.candidateName
      case _ => false
    }

    override def hashCode = tpe.hashCode

    lazy val cleanReason = {
      val clean = reason
        .stripPrefix(Messages.typingTypeApply)
        .stripPrefix(Messages.hasMatching)
      if (cleanCandidate == "Lazy.mkLazy" &&
        clean.startsWith("Unable to derive"))
          "Unable to derive lazy implicit"
      else clean
    }
  }

  var implicitTypes = List[Type]()
  var implicitErrors = List[ImpError]()

  def nestedImplicit = implicitTypes.nonEmpty

  def search
  (tree: Tree, pt: Type, isView: Boolean, context: Context, pos: Position) = {
    if (!nestedImplicit) implicitErrors = List()
    implicitTypes = pt :: implicitTypes
    val result =
      new ImplicitSearch2(tree, pt, isView, context, pos).bestImplicit
    if (result.isSuccess)
      implicitErrors = implicitErrors.dropWhile(_.tpe == pt)
    implicitTypes = implicitTypes.tail
    result
  }

  def inferImplicit2(tree: Tree, pt: Type, reportAmbiguous: Boolean,
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
    if (shouldPrint)
      typingStack.printTyping(tree, "typing implicit: %s %s"
        .format(tree, context.undetparamsString))
    val implicitSearchContext = context.makeImplicit(reportAmbiguous)
    val result = search(tree, pt, isView, implicitSearchContext, pos)
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
      val prev = implicitTypes.headOption.getOrElse(typeOf[Unit])
      implicitErrors = ImpError(pt, what, reason, prev) :: implicitErrors
      super.failure(what, reason, pos)
    }

    override val infer =
      if (featureBounds) new Inferencer2
      else new Inferencer {
        def context = ImplicitSearch2.this.context

        override def isCoercible(tp: Type, pt: Type) =
          undoLog undo viewExists(tp, pt)
      }

    class Inferencer2
    extends Inferencer
    {
      import InferErrorGen._

      def context = ImplicitSearch2.this.context

      override def isCoercible(tp: Type, pt: Type) =
        undoLog undo viewExists(tp, pt)

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
          val params = bracket(tparams.map(_.defString))
          val tpes = bracket(targs.map(formatInfix(_, true)))
          val msg = s"nonconformant bounds;\n${tpes.red}\n${params.green}"
          ErrorUtils.issueNormalTypeError(tree, msg)
      }
    }
  }

  def noImplicitError(tree: Tree, param: Symbol)
  (implicit context: Context): Unit = {
    val err = formatImplicitMessage(param, !nestedImplicit, implicitErrors)
    ErrorUtils.issueNormalTypeError(tree, err)
  }

  override def NoImplicitFoundError(tree: Tree, param: Symbol)
  (implicit context: Context): Unit = {
    val msg = param.tpe.typeSymbolDirect match {
      case ImplicitNotFoundMsg(_) => true
      case _ => false
    }
    if (featureImplicits && !msg) noImplicitError(tree, param)
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
      def featureBounds = boolean(keyBounds)
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

  val keyAll = "all"
  val keyImplicits = "implicits"
  val keyFoundReq = "foundreq"
  val keyInfix = "infix"
  val keyBounds = "bounds"

  val opts: mutable.Map[String, String] = mutable.Map(
    keyAll -> "true",
    keyImplicits -> "true",
    keyFoundReq -> "true",
    keyInfix -> "true",
    keyBounds -> "false"
  )

  def opt(key: String, default: String) = opts.getOrElse(key, default)

  def enabled = opt("all", "true") == "true"

  def boolean(key: String) = enabled && opt(key, "true") == "true"
}
