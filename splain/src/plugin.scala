package splain

import tools.nsc._

import StringColor._

sealed trait Formatted

case class Infix(infix: Formatted, left: Formatted, right: Formatted,
  top: Boolean)
extends Formatted

case class Simple(tpe: String)
extends Formatted

case object UnitForm
extends Formatted

case class Applied(cons: Formatted, args: List[Formatted])
extends Formatted

case class TupleForm(elems: List[Formatted])
extends Formatted

case class FunctionForm(args: List[Formatted], ret: Formatted, top: Boolean)
extends Formatted

object FunctionForm
{
  def fromArgs(args: List[Formatted], top: Boolean) = {
    val (params, returnt) = args.splitAt(args.length - 1)
    FunctionForm(params, returnt.headOption.getOrElse(UnitForm), top)
  }
}

trait Formatting
extends Compat
{ self: Analyzer =>
  import global._

  def featureInfix: Boolean
  def featureColor: Boolean

  implicit def colors =
    if(featureColor) StringColors.color
    else StringColors.noColor

  def dealias(tpe: Type) =
    if (isAux(tpe)) tpe
    else tpe.dealias

  def isRefined(tpe: Type) = tpe.dealias match {
    case RefinedType(_, _) => true
    case _ => false
  }

  def isSymbolic(tpe: Type) = {
    val n = tpe.typeConstructor.typeSymbol.name
    !isRefined(tpe) && (n.encodedName.toString != n.decodedName.toString)
  }

  def ctorNames(tpe: Type): List[String] =
    scala.util.Try(tpe.typeConstructor.toString)
      .map(_.split('.').toList)
      .getOrElse(List(tpe.toString))

  def isAux(tpe: Type) =
    OptionOps.contains("Aux")(ctorNames(tpe).lastOption)

  def formatRefinement(sym: Symbol) = {
    if (sym.hasRawInfo) {
      val rhs = showFormatted(formatType(sym.rawInfo, true))
      s"$sym = $rhs"
    }
    else sym.toString
  }

  def formatAuxSimple(tpe: Type) =
    ctorNames(tpe).takeRight(2).mkString(".")

  def formatNormalSimple(tpe: Type) =
    tpe match {
    case RefinedType(parents, decls) =>
      val simple =
        parents.map(a => showFormatted(formatType(a, true))).mkString(" with ")
      val refine = decls.map(formatRefinement).mkString("; ")
      s"$simple {$refine}"
    case a =>
      val name = a.typeSymbol.name.decodedName.toString
      if (a.typeSymbol.isModuleClass) s"$name.type"
      else name
  }

  def formatSimpleType(tpe: Type) = {
    if (isAux(tpe)) formatAuxSimple(tpe)
    else formatNormalSimple(tpe)
  }

  def formatTypeApply(simple: String, args: List[String]) = {
    val a = bracket(args)
    s"$simple$a"
  }

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

  def showFormatted(tpe: Formatted): String = tpe match {
    case Simple(a) => a
    case Applied(cons, args) =>
      formatTypeApply(showFormatted(cons), args map showFormatted)
    case Infix(infix, left, right, top) =>
      val l = showFormatted(left)
      val i = showFormatted(infix)
      val r = showFormatted(right)
      wrapParens(s"$l $i $r", top)
    case UnitForm => "Unit"
    case FunctionForm(args, ret, top) =>
      val a = formatTuple(args map showFormatted)
      val r = showFormatted(ret)
      wrapParens(s"$a => $r", top)
    case TupleForm(elems) => formatTuple(elems map showFormatted)
  }

  def wrapParens(expr: String, top: Boolean) =
    if (top) expr else s"($expr)"

  def formatInfix[A](tpe: Type, args: List[A], top: Boolean,
    rec: A => Boolean => Formatted): Formatted = {
      val simple = formatSimpleType(tpe)
      def formattedArgs = args.map(rec(_)(true))
      if (simple.startsWith("Function"))
        FunctionForm.fromArgs(formattedArgs, top)
      else if (simple.startsWith("Tuple")) TupleForm(formattedArgs)
      else args match {
        case left :: right :: Nil if isSymbolic(tpe) =>
          val l = rec(left)(false)
          val r = rec(right)(false)
          Infix(Simple(simple), l, r, top)
        case head :: tail =>
          Applied(Simple(simple), formattedArgs)
        case _ =>
          Simple(simple)
      }
  }

  def formatType(tpe: Type, top: Boolean): Formatted = {
    val dtpe = dealias(tpe)
    val rec = (tp: Type) => (t: Boolean) => formatType(tp, t)
    if (featureInfix) formatInfix(dtpe, dtpe.typeArgs, top, rec)
    else Simple(dtpe.toLongString)
  }

  def formatDiff(found: Type, req: Type, top: Boolean): Formatted = {
    val (left, right) = dealias(found) -> dealias(req)
    if (left.typeSymbol == right.typeSymbol) {
      val rec = (l: Type, r: Type) => (t: Boolean) => formatDiff(l, r, t)
      val recT = rec.tupled
      val args = left.typeArgs zip right.typeArgs
      formatInfix(left, args, top, recT)
    }
    else {
      val l = formatType(left, true)
      val r = formatType(right, true)
      Simple(s"${showFormatted(l).red}|${showFormatted(r).green}")
    }
  }

  def formatNestedImplicit(err: ImpError): List[String] = {
    val candidate = err.cleanCandidate
    val tpe = showFormatted(formatType(err.tpe, true))
    val extraInfo =
      if (tpe == showFormatted(formatType(err.prev, true))) ""
      else s" as ${tpe.green}"
    val problem = s"${candidate.red} invalid$extraInfo because"
    List(problem, err.cleanReason)
  }

  def hideImpError(error: ImpError) = {
    error.candidateName.toString == "mkLazy"
  }

  def formatNestedImplicits(errors: List[ImpError]) = {
    errors.distinct filterNot hideImpError flatMap formatNestedImplicit
    // errors filterNot hideImpError flatMap formatNestedImplicit
  }

  def formatImplicitParam(sym: Symbol) = sym.name.toString

  def overrideMessage(msg: String): Option[String] = {
    if (msg.startsWith(Messages.lazyderiv)) None
    else Some(msg)
  }

  def effectiveImplicitType(tpe: Type) = {
    if (tpe.typeSymbol.name.toString == "Lazy")
      tpe.typeArgs.headOption.getOrElse(tpe)
    else tpe
  }

  def formatImplicitMessage
  (param: Symbol, showStack: Boolean, errors: List[ImpError]) = {
    val tpe = param.tpe
    val msg = tpe.typeSymbolDirect match {
      case ImplicitNotFoundMsg(msg) =>
        overrideMessage(msg.format(TermName(param.name.toString), tpe))
          .map(a => s" ($a)")
          .getOrElse("")
      case _ => ""
    }
    val effTpe = effectiveImplicitType(tpe)
    def stack = formatNestedImplicits(errors)
    val paramName = formatImplicitParam(param)
    val ptp = showFormatted(formatType(effTpe, true))
    val nl = if (showStack && errors.nonEmpty) "\n" else ""
    val ex = if(showStack) stack.mkString("\n") else ""
    val pre = if (showStack) "implicit error;\n" else ""
    val bang = "!"
    val i = "I"
    s"${pre}${bang.red}${i.blue} ${paramName.yellow}$msg: ${ptp.green}$nl$ex"
  }
}

trait TypeDiagnostics
extends typechecker.TypeDiagnostics
with Formatting
{ self: Analyzer =>
  import global._

  def featureFoundReq: Boolean

  def foundReqMsgShort(found: Type, req: Type): String =
    showFormatted(formatDiff(found, req, true))

  // FIXME not used, needs feature
  def foundReqMsgNormal(found: Type, req: Type): String = {
    withDisambiguation(Nil, found, req)(
      showFormatted(formatDiff(found, req, true))) +
        explainVariance(found, req) +
        explainAnyVsAnyRef(found, req)
  }

  override def foundReqMsg(found: Type, req: Type): String =
    if (featureFoundReq) ";\n  " + foundReqMsgShort(found, req)
    else super.foundReqMsg(found, req)
}

trait Analyzer
extends typechecker.Analyzer
with ImplicitsCompat
with TypeDiagnostics
