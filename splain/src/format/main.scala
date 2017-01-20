package splain

import tools.nsc._

import StringColor._

trait Formatters
{ self: Analyzer =>
  import global._

  def formatType(tpe: Type, top: Boolean): Formatted

  trait SpecialFormatter
  {
    def apply[A](tpe: Type, simple: String, args: List[A],
      formattedArgs: List[Formatted], top: Boolean,
      rec: A => Boolean => Formatted): Option[Formatted]
  }

  object FunctionFormatter
  extends SpecialFormatter
  {
    def apply[A](tpe: Type, simple: String, args: List[A],
      formattedArgs: List[Formatted], top: Boolean,
      rec: A => Boolean => Formatted) = {
        if (simple.startsWith("Function"))
          Some(FunctionForm.fromArgs(formattedArgs, top))
        else None
    }
  }

  object TupleFormatter
  extends SpecialFormatter
  {
    def apply[A](tpe: Type, simple: String, args: List[A],
      formattedArgs: List[Formatted], top: Boolean,
      rec: A => Boolean => Formatted) = {
        if (simple.startsWith("Tuple"))
          Some(TupleForm(formattedArgs))
        else None
    }
  }

  object SLRecordItemFormatter
  extends SpecialFormatter
  {
    def keyTagName = "shapeless.labelled.KeyTag"

    def taggedName = "shapeless.tag.Tagged"

    def isKeyTag(tpe: Type) = tpe.typeSymbol.fullName == keyTagName

    def isTagged(tpe: Type) = tpe.typeSymbol.fullName == taggedName

    object extractStringConstant
    {
      def unapply(tpe: Type) = tpe match {
        case ConstantType(Constant(a: String)) => Some(a)
        case _ => None
      }
    }

    def formatConstant(tag: String): PartialFunction[Type, String] = {
      case a if a == typeOf[scala.Symbol] =>
        s"'$tag"
    }

    def formatKeyArg: PartialFunction[List[Type], Option[Formatted]] = {
      case RefinedType(parents, _) :: _ :: Nil =>
        for {
          main <- parents.headOption
          tagged <- parents.find(isTagged)
          headArg <- tagged.typeArgs.headOption
          tag <- extractStringConstant.unapply(headArg)
          repr <- formatConstant(tag).lift(main)
        } yield Simple(repr)
      case extractStringConstant(tag) :: _ :: Nil =>
        Some(Simple(s""""$tag""""))
      case tag :: _ :: Nil =>
        Some(formatType(tag, true))
    }

    def formatKey(tpe: Type): Formatted = {
      formatKeyArg.lift(tpe.typeArgs).flatten getOrElse formatType(tpe, true)
    }

    def apply[A](tpe: Type, simple: String, args: List[A],
      formattedArgs: List[Formatted], top: Boolean,
      rec: A => Boolean => Formatted) = {
        tpe match {
          case RefinedType(actual :: key :: Nil, _) if isKeyTag(key) =>
            Some(SLRecordItem(formatKey(key), formatType(actual, true)))
          case _ =>
            None
        }
    }
}
}

trait Formatting
extends Compat
with Formatters
{ self: Analyzer =>
  import global._

  def featureInfix: Boolean
  def featureBreakInfix: Option[Int]
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
      val rhs = showType(sym.rawInfo)
      s"$sym = $rhs"
    }
    else sym.toString
  }

  def formatAuxSimple(tpe: Type) =
    ctorNames(tpe).takeRight(2).mkString(".")

  def formatNormalSimple(tpe: Type) =
    tpe match {
    case RefinedType(parents, decls) =>
      val simple = parents map showType mkString " with "
      val refine = decls map formatRefinement mkString "; "
      s"$simple {$refine}"
    case a =>
      val name = a.typeSymbol.name.decodedName.toString
      if (a.typeSymbol.isModuleClass) s"$name.type"
      else name
  }

  def formatSimpleType(tpe: Type): String = {
    if (isAux(tpe)) formatAuxSimple(tpe)
    else formatNormalSimple(tpe)
  }

  def indent(lines: List[String]) = lines map ("  " + _)

  /**
   * If the args of an applied type constructor are multiline, create separate
   * lines for the constructor name and the closing bracket; else return a
   * single line.
   */
  def showTypeApply
  (cons: String, args: List[TypeRepr], break: Boolean)
  : TypeRepr = {
    val flatArgs = bracket(args map (_.flat))
    val flat = FlatType(s"$cons$flatArgs")
    def brokenArgs = args match {
      case head :: tail =>
        tail.foldLeft(head.lines)((z, a) => z ::: "," :: a.lines)
      case _ => Nil
    }
    def broken = BrokenType(s"$cons[" :: indent(brokenArgs) ::: List("]"))
    if (break) decideBreak(flat, broken) else flat
  }

  def showTuple(args: List[String]) =
    args match {
      case head :: Nil => head
      case _ => args.mkString("(", ",", ")")
    }

  def showSLRecordItem(key: Formatted, value: Formatted) = {
    FlatType(
      s"(${showFormattedNoBreak(key)} ->> ${showFormattedNoBreak(value)})")
  }

  def bracket[A](params: List[A]) = params.mkString("[", ", ", "]")

  def formatFunction(args: List[String]) = {
    val (params, returnt) = args.splitAt(args.length - 1)
    s"${showTuple(params)} => ${showTuple(returnt)}"
  }

  def decideBreak
  (flat: FlatType, broken: => BrokenType)
  : TypeRepr = {
    featureBreakInfix
      .flatMap { maxlen =>
        if (flat.length > maxlen) Some(broken)
        else None
      }
      .getOrElse(flat)
  }

  /**
   * Turn a nested infix type structure into a flat list
   * ::[A, ::[B, C]]] => List(A, ::, B, ::, C)
   */
  def flattenInfix(tpe: Infix): List[Formatted] = {
    def step(tpe: Formatted): List[Formatted] = tpe match {
      case Infix(infix, left, right, top) =>
        left :: infix :: step(right)
      case a => List(a)
    }
    step(tpe)
  }

  /**
   * Break a list produced by [[flattenInfix]] into lines by taking two
   * elements at a time, then appending the terminal.
   * If the expression's length is smaller than the threshold specified via
   * plugin parameter, return a single line.
   */
  def breakInfix(types: List[Formatted]): TypeRepr = {
    val form = types map showFormattedLBreak
    def broken: List[String] = form
      .sliding(2, 2)
      .toList
      .flatMap {
        case left :: right :: Nil =>
          (left, right) match {
            case (FlatType(tpe), FlatType(infix)) =>
              List(s"$tpe $infix")
            case _ => left.lines ++ right.lines
          }
        case last :: Nil => last.lines
        // for exhaustiveness, cannot be reached
        case l => l.flatMap(_.lines)
      }
    val flat = FlatType(form.flatMap(_.lines) mkString " ")
    decideBreak(flat, BrokenType(broken))
  }

  def showFormattedL(tpe: Formatted, break: Boolean): TypeRepr =
    tpe match {
      case Simple(a) => FlatType(a)
      case Applied(cons, args) =>
        val reprs = args map (showFormattedL(_, break))
        showTypeApply(showFormattedNoBreak(cons), reprs, break)
      case tpe @ Infix(infix, left, right, top) =>
        val flat = flattenInfix(tpe)
        val broken: TypeRepr =
          if (break) breakInfix(flat)
          else FlatType(flat map showFormattedNoBreak mkString " ")
        wrapParensRepr(broken, top)
      case UnitForm => FlatType("Unit")
      case FunctionForm(args, ret, top) =>
        val a = showTuple(args map showFormattedNoBreak)
        val r = showFormattedNoBreak(ret)
        FlatType(wrapParens(s"$a => $r", top))
      case TupleForm(elems) =>
        FlatType(showTuple(elems map showFormattedNoBreak))
      case SLRecordItem(key, value) =>
        showSLRecordItem(key, value)
    }

  def showFormattedLBreak(tpe: Formatted) = showFormattedL(tpe, true)

  def showFormattedLNoBreak(tpe: Formatted) = showFormattedL(tpe, false)

  def showFormatted(tpe: Formatted, break: Boolean): String =
    showFormattedL(tpe, break).joinLines

  def showFormattedNoBreak(tpe: Formatted) =
    showFormattedLNoBreak(tpe).tokenize

  def showType(tpe: Type) = showFormatted(formatType(tpe, true), false)

  def showTypeBreak(tpe: Type) = showFormatted(formatType(tpe, true), true)

  def wrapParens(expr: String, top: Boolean) =
    if (top) expr else s"($expr)"

  def wrapParensRepr(tpe: TypeRepr, top: Boolean): TypeRepr = {
    tpe match {
      case FlatType(tpe) => FlatType(wrapParens(tpe, top))
      case BrokenType(lines) =>
        if (top) tpe else BrokenType("(" :: indent(lines) ::: List(")"))
    }
  }

  val specialFormatters: List[SpecialFormatter] =
    List(
      FunctionFormatter,
      TupleFormatter,
      SLRecordItemFormatter
    )

  def formatSpecial[A](tpe: Type, simple: String, args: List[A],
    formattedArgs: List[Formatted], top: Boolean,
    rec: A => Boolean => Formatted)
  : Option[Formatted] = {
    specialFormatters
      .map(_.apply(tpe, simple, args, formattedArgs, top, rec))
      .collectFirst { case Some(a) => a }
      .headOption
  }

  def formatInfix[A](tpe: Type, args: List[A], top: Boolean,
    rec: A => Boolean => Formatted): Formatted = {
      val simple = formatSimpleType(tpe)
      def formattedArgs = args.map(rec(_)(true))
      formatSpecial(tpe, simple, args, formattedArgs, top, rec) getOrElse {
        args match {
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
  }

  def formatType(tpe: Type, top: Boolean): Formatted = {
    val dtpe = dealias(tpe)
    val rec = (tp: Type) => (t: Boolean) => formatType(tp, t)
    if (featureInfix) formatInfix(dtpe, dtpe.typeArgs, top, rec)
    else Simple(dtpe.toLongString)
  }

  def formatDiff(found: Type, req: Type, top: Boolean): Formatted = {
    val (left, right) = dealias(found) -> dealias(req)
    if (left =:= right) formatType(left, top)
    else if (left.typeSymbol == right.typeSymbol) {
      val rec = (l: Type, r: Type) => (t: Boolean) => formatDiff(l, r, t)
      val recT = rec.tupled
      val args = left.typeArgs zip right.typeArgs
      formatInfix(left, args, top, recT)
    }
    else {
      val l = showType(left)
      val r = showType(right)
      Simple(s"${l.red}|${r.green}")
    }
  }

  def formatNonConfBounds(err: NonConfBounds) = {
    val params = bracket(err.tparams.map(_.defString))
    val tpes = bracket(err.targs map showType)
    s"nonconformant bounds;\n${tpes.red}\n${params.green}"
  }

  def formatNestedImplicit(err: ImpFailReason): List[String] = {
    val candidate = err.cleanCandidate
    val problem = s"${candidate.red} invalid because"
    val reason = err match {
      case e: ImpError => implicitMessage(e.param)
      case e: NonConfBounds => formatNonConfBounds(e)
    }
    List(problem, reason)
  }

  def hideImpError(error: ImpFailReason) =
    error.candidateName.toString == "mkLazy"

  /**
   * Remove duplicates and special cases that should not be shown.
   * In some cases, candidates are reported twice, once as `Foo.f` and once as
   * `f`. [[ImpFailReason.equals]] checks the simple names for identity, which
   * is suboptimal, but works for 99% of cases.
   * Special cases are handled in [[hideImpError]]
   */
  def formatNestedImplicits(errors: List[ImpFailReason]) =
    errors.distinct filterNot hideImpError flatMap formatNestedImplicit

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

  def implicitMessage(param: Symbol) = {
    val tpe = param.tpe
    val msg = tpe.typeSymbolDirect match {
      case ImplicitNotFoundMsg(msg) =>
        overrideMessage(msg.format(TermName(param.name.toString), tpe))
          .map(a => s" ($a)")
          .getOrElse("")
      case _ => ""
    }
    val effTpe = effectiveImplicitType(tpe)
    val paramName = formatImplicitParam(param)
    val ptp = showTypeBreak(effTpe)
    val bang = "!"
    val i = "I"
    s"${bang.red}${i.blue} ${paramName.yellow}$msg: ${ptp.green}"
  }

  def formatImplicitError(param: Symbol, errors: List[ImpFailReason]) = {
    val stack = formatNestedImplicits(errors)
    val nl = if (errors.nonEmpty) "\n" else ""
    val ex = stack.mkString("\n")
    val pre = "implicit error;\n"
    val msg = implicitMessage(param)
    s"$pre$msg$nl$ex"
  }
}
