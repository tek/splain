package splain

import scala.collection.mutable
import scala.util.matching.Regex

import StringColor._

class FormatCache[K, V](cache: mutable.Map[K, V], var hits: Long)
{
  def apply(k: K, orElse: => V) = {
    if (cache.contains(k)) hits += 1
    cache.getOrElseUpdate(k, orElse)
  }

  def stats = s"${cache.size}/$hits"
}

object FormatCache
{
  def apply[K, V] = new FormatCache[K, V](mutable.Map(), 0)
}

trait Formatting
extends Formatters
with ImplicitMsgCompat
{ self: Analyzer =>
  import global._

  def featureInfix: Boolean
  def featureBreakInfix: Option[Int]
  def featureColor: Boolean
  def featureCompact: Boolean
  def featureTree: Boolean
  def featureBoundsImplicits: Boolean
  def featureTruncRefined: Option[Int]
  def featureRewrite: String
  def featureKeepModules: Int

  implicit def colors =
    if(featureColor) StringColors.color
    else StringColors.noColor

  def dealias(tpe: Type) =
    if (isAux(tpe)) tpe
    else {
      val actual = tpe match {
        case ExistentialType(_, t) => t
        case _ => tpe
      }
      actual.dealias
    }

  def extractArgs(tpe: Type) = {
    tpe match {
      case PolyType(params, result) =>
        result.typeArgs.map {
          case t if params.contains(t.typeSymbol) => WildcardType
          case a => a
        }
      case t: AliasTypeRef if !isAux(tpe) =>
        t.betaReduce.typeArgs.map(a => if (a.typeSymbolDirect.isTypeParameter) WildcardType else a)
      case _ => tpe.typeArgs
    }
  }

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

  def isAux(tpe: Type) = ctorNames(tpe).lastOption.contains("Aux")

  def formatRefinement(sym: Symbol) = {
    if (sym.hasRawInfo) {
      val rhs = showType(sym.rawInfo)
      s"$sym = $rhs"
    }
    else sym.toString
  }

  def formatAuxSimple(tpe: Type) = {
    val names = ctorNames(tpe)
    val num = if (names.lift(names.length - 2).contains("Case")) 3 else 2
    ctorNames(tpe) takeRight num mkString "."
  }

  def rewriteRegexes: List[(Regex, String)] =
    featureRewrite
      .split(";")
      .toList
      .map(_.split("/").toList)
      .map {
        case re :: repl :: _ =>
          (re.r, repl)
        case re :: Nil =>
          (re.r, "")
        case Nil =>
          ("".r, "")
      }

  case class MatchRewrite(target: String)
  {
    def unapply(candidate: (Regex, String)): Option[String] = {
      val (regex, replacement) = candidate
      val transformed = regex.replaceAllIn(target, replacement)
      if (transformed == target || transformed.isEmpty) None
      else Some(transformed)
    }
  }

  def modulePath: Type => List[String] = {
    case tr: TypeRef if !tr.pre.toString.isEmpty =>
      tr.pre.toString.split("\\.").toList.takeWhile(_ != "type")
    case _ =>
      Nil
  }

  def pathPrefix: List[String] => String = {
    case Nil =>
      ""
    case a =>
      a.mkString("", ".", ".")
  }

  def stripModules(path: List[String], name: String)(keep: Int): String =
    s"${pathPrefix(path.takeRight(keep))}$name"

  def stripType(tpe: Type): String = {
    val sym = if (tpe.takesTypeArgs) tpe.typeSymbolDirect else tpe.typeSymbol
    val symName = sym.name.decodedName.toString
    val path = modulePath(tpe)
    val fullType = s"${pathPrefix(path)}$symName"
    val matchRewrite = MatchRewrite(fullType)
    val regexRewritten =
      rewriteRegexes
        .foldLeft(fullType) { case (current, (regex, replacement)) => regex.replaceAllIn(current, replacement) }
    val stripped =
      if (fullType == regexRewritten || regexRewritten.isEmpty) stripModules(path, symName)(featureKeepModules)
      else regexRewritten
    if (sym.isModuleClass) s"$stripped.type"
    else stripped
  }

  def formatNormalSimple(tpe: Type) =
    tpe match {
    case RefinedType(parents, decls) =>
      val simple = parents map showType mkString " with "
      val refine = decls map formatRefinement mkString "; "
      val refine1 = featureTruncRefined.collect { case len if (refine.length > len) => "..." }.getOrElse(refine)
      s"$simple {$refine1}"
    case a @ WildcardType => a.toString
    case a =>
      stripType(a)
  }

  def formatSimpleType(tpe: Type): String =
    if (isAux(tpe)) formatAuxSimple(tpe)
    else formatNormalSimple(tpe)

  def indentLine(line: String, n: Int = 1, prefix: String = "  ") = (prefix * n) + line

  def indent(lines: List[String], n: Int = 1, prefix: String = "  ") = lines map (indentLine(_, n, prefix))

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

  def decideBreak(flat: FlatType, broken: => BrokenType)
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
      case Infix(infix, left, right, _) =>
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

  val showFormattedLCache = FormatCache[(Formatted, Boolean), TypeRepr]

  def showFormattedLImpl(tpe: Formatted, break: Boolean): TypeRepr =
    tpe match {
      case Simple(a) => FlatType(a)
      case Applied(cons, args) =>
        val reprs = args map (showFormattedL(_, break))
        showTypeApply(showFormattedNoBreak(cons), reprs, break)
      case tpe @ Infix(_, _, _, top) =>
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
      case Diff(left, right) =>
        val l = showFormattedNoBreak(left)
        val r = showFormattedNoBreak(right)
        FlatType(s"${l.red}|${r.green}")
    }

  def showFormattedL(tpe: Formatted, break: Boolean): TypeRepr = {
    val key = (tpe, break)
    showFormattedLCache(key, showFormattedLImpl(tpe, break))
  }

  def showFormattedLBreak(tpe: Formatted) = showFormattedL(tpe, true)

  def showFormattedLNoBreak(tpe: Formatted) = showFormattedL(tpe, false)

  def showFormatted(tpe: Formatted, break: Boolean): String =
    showFormattedL(tpe, break).joinLines

  def showFormattedNoBreak(tpe: Formatted) =
    showFormattedLNoBreak(tpe).tokenize

  def showType(tpe: Type): String = showFormatted(formatType(tpe, true), false)

  def showTypeBreak(tpe: Type): String = showFormatted(formatType(tpe, true), true)

  def showTypeBreakL(tpe: Type): List[String] = showFormattedL(formatType(tpe, true), true).lines

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

  def formatSpecial[A](tpe: Type, simple: String, args: List[A], formattedArgs: => List[Formatted], top: Boolean,
    rec: A => Boolean => Formatted)
  : Option[Formatted] = {
    specialFormatters
      .map(_.apply(tpe, simple, args, formattedArgs, top, rec))
      .collectFirst { case Some(a) => a }
      .headOption
  }

  def formatInfix[A](simple: String, left: A, right: A, top: Boolean, rec: A => Boolean => Formatted) = {
      val l = rec(left)(false)
      val r = rec(right)(false)
      Infix(Simple(simple), l, r, top)
  }

  def formatWithInfix[A](tpe: Type, args: List[A], top: Boolean, rec: A => Boolean => Formatted): Formatted = {
      val simple = formatSimpleType(tpe)
      lazy val formattedArgs = args.map(rec(_)(true))
      formatSpecial(tpe, simple, args, formattedArgs, top, rec) getOrElse {
        args match {
          case left :: right :: Nil if isSymbolic(tpe) =>
            formatInfix(simple, left, right, top, rec)
          case _ :: _ =>
            Applied(Simple(simple), formattedArgs)
          case _ =>
            Simple(simple)
        }
      }
  }

  def formatTypeImpl(tpe: Type, top: Boolean): Formatted = {
    val dtpe = dealias(tpe)
    val rec = (tp: Type) => (t: Boolean) => formatType(tp, t)
    if (featureInfix) formatWithInfix(dtpe, extractArgs(dtpe), top, rec)
    else Simple(dtpe.toLongString)
  }

  val formatTypeCache = FormatCache[(Type, Boolean), Formatted]

  def formatType(tpe: Type, top: Boolean): Formatted = {
    val key = (tpe, top)
    formatTypeCache(key, formatTypeImpl(tpe, top))
  }

  def formatDiffInfix(left: Type, right: Type, top: Boolean) = {
    val rec = (l: Type, r: Type) => (t: Boolean) => formatDiff(l, r, t)
    val recT = rec.tupled
    val args = extractArgs(left) zip extractArgs(right)
    formatWithInfix(left, args, top, recT)
  }

  def formatDiffSpecial(left: Type, right: Type, top: Boolean) = {
    specialFormatters.map(_.diff(left, right, top))
      .collectFirst { case Some(a) => a }
      .headOption
  }

  def formatDiffSimple(left: Type, right: Type) = {
    val l = formatType(left, true)
    val r = formatType(right, true)
    Diff(l, r)
  }

  def formatDiffImpl(found: Type, req: Type, top: Boolean): Formatted = {
    val (left, right) = dealias(found) -> dealias(req)
    if (left =:= right)
      formatType(left, top)
    else if (left.typeSymbol == right.typeSymbol)
      formatDiffInfix(left, right, top)
    else
      formatDiffSpecial(left, right, top) getOrElse
        formatDiffSimple(left, right)
  }

  val formatDiffCache = FormatCache[(Type, Type, Boolean), Formatted]

  def formatDiff(left: Type, right: Type, top: Boolean) = {
    val key = (left, right, top)
    formatDiffCache(key, formatDiffImpl(left, right, top))
  }

  // TODO split non conf bounds
  def formatNonConfBounds(err: NonConfBounds): List[String] = {
    val params = bracket(err.tparams.map(_.defString))
    val tpes = bracket(err.targs map showType)
    List("nonconformant bounds;", tpes.red, params.green)
  }

  def formatNestedImplicit(err: ImpFailReason): (String, List[String], Int) = {
    val candidate = err.cleanCandidate
    val problem = s"${candidate.red} invalid because"
    val reason = err match {
      case e: ImpError => implicitMessage(e.param)
      case e: NonConfBounds => formatNonConfBounds(e)
    }
    (problem, reason, err.nesting)
  }

  def hideImpError(error: ImpFailReason) =
    (error.candidateName.toString == "mkLazy") || (!featureBoundsImplicits && (
      error match {
        case NonConfBounds(_, _, _, _, _) => true
        case _ => false
      }
      ))

  def indentTree(tree: List[(String, List[String], Int)], baseIndent: Int): List[String] = {
    val nestings = tree.map(_._3).distinct.sorted
    tree
      .flatMap {
        case (head, tail, nesting) =>
          val ind = baseIndent + nestings.indexOf(nesting).abs
          indentLine(head, ind, "――") :: indent(tail, ind)
      }
  }

  def formatIndentTree(chain: List[ImpFailReason], baseIndent: Int) = {
    val formatted = chain map formatNestedImplicit
    indentTree(formatted, baseIndent)
  }

  def deepestLevel(chain: List[ImpFailReason]) = {
    chain.foldLeft(0)((z, a) => if (a.nesting > z) a.nesting else z)
  }

  def formatImplicitChainTreeCompact(chain: List[ImpFailReason]): Option[List[String]] = {
    chain
      .headOption
      .map { head =>
        val max = deepestLevel(chain)
        val leaves = chain.drop(1).dropWhile(_.nesting < max)
        val base = if (head.nesting == 0) 0 else 1
        val (fhh, fht, fhn) = formatNestedImplicit(head)
        val spacer = if (leaves.nonEmpty && leaves.length < chain.length) List("⋮".blue) else Nil
        val fh = (fhh, fht ++ spacer, fhn)
        val ft = leaves map formatNestedImplicit
        indentTree(fh :: ft, base)
      }
  }

  def formatImplicitChainTreeFull(chain: List[ImpFailReason]): List[String] = {
    val baseIndent = chain.headOption.map(_.nesting).getOrElse(0)
    formatIndentTree(chain, baseIndent)
  }

  def formatImplicitChainFlat(chain: List[ImpFailReason]): List[String] = {
    chain map formatNestedImplicit flatMap { case (h, t, _) => h :: t }
  }

  def formatImplicitChainTree(chain: List[ImpFailReason]): List[String] = {
    val compact = if (featureCompact) formatImplicitChainTreeCompact(chain) else None
    compact getOrElse formatImplicitChainTreeFull(chain)
  }

  def formatImplicitChain(chain: List[ImpFailReason]): List[String] = {
    if (featureTree) formatImplicitChainTree(chain)
    else formatImplicitChainFlat(chain)
  }

  /**
   * Remove duplicates and special cases that should not be shown.
   * In some cases, candidates are reported twice, once as `Foo.f` and once as
   * `f`. `ImpFailReason.equals` checks the simple names for identity, which
   * is suboptimal, but works for 99% of cases.
   * Special cases are handled in [[hideImpError]]
   */
  def formatNestedImplicits(errors: List[ImpFailReason]) = {
    val visible = errors filterNot hideImpError
    val chains = splitChains(visible).map(_.distinct).distinct
    chains map formatImplicitChain flatMap ("" :: _) drop 1
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

  def implicitMessage(param: Symbol): List[String] = {
    val tpe = param.tpe
    val msg = tpe.typeSymbolDirect match {
      case ImplicitNotFoundMsg(msg) =>
        overrideMessage(formatMsg(msg, param, tpe))
          .map(a => s" ($a)")
          .getOrElse("")
      case _ => ""
    }
    val effTpe = effectiveImplicitType(tpe)
    val paramName = formatImplicitParam(param)
    val bang = "!"
    val i = "I"
    val head = s"${bang.red}${i.blue} ${paramName.yellow}$msg:"
    showTypeBreakL(effTpe) match {
      case single :: Nil => List(s"$head ${single.green}")
      case l => head :: indent(l).map(_.green)
    }
  }

  def splitChains(errors: List[ImpFailReason]): List[List[ImpFailReason]] = {
    errors.foldRight(Nil: List[List[ImpFailReason]]) {
      case (a, chains @ ((chain @ (prev :: _)) :: tail)) =>
        if (a.nesting > prev.nesting) List(a) :: chains
        else (a :: chain) :: tail
      case (a, _) =>
        List(List(a))
    }
  }

  def formatImplicitError(param: Symbol, errors: List[ImpFailReason]) = {
    val stack = formatNestedImplicits(errors)
    val nl = if (errors.nonEmpty) "\n" else ""
    val ex = stack.mkString("\n")
    val pre = "implicit error;\n"
    val msg = implicitMessage(param).mkString("\n")
    s"$pre$msg$nl$ex"
  }

  def cacheStats = {
    val sfl = showFormattedLCache.stats
    val ft = formatTypeCache.stats
    val df = formatDiffCache.stats
    s"showFormatted -> $sfl, formatType -> $ft, formatDiff -> $df"
  }
}
