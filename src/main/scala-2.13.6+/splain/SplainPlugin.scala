package splain

import collection.mutable
import tools.nsc._
import typechecker.splain._

trait SplainPluginLike extends plugins.Plugin {

  val name = "splain"
  val description = "better types and implicit errors"
  val components = Nil

  val keyAll = "all"
  val keyImplicits = "implicits"
  val keyFoundReq = "foundreq"
  val keyInfix = "infix"
  val keyBounds = "bounds"
  val keyColor = "color"
  val keyBreakInfix = "breakinfix"
  val keyCompact = "compact"
  val keyTree = "tree"
  val keyBoundsImplicits = "boundsimplicits"
  val keyTruncRefined = "truncrefined"
  val keyRewrite = "rewrite"
  val keyKeepModules = "keepmodules"

  val opts: mutable.Map[String, String] = mutable.Map(
    keyAll -> "true",
    keyImplicits -> "true",
    keyFoundReq -> "true",
    keyInfix -> "true",
    keyBounds -> "false",
    keyColor -> "true",
    keyBreakInfix -> "0",
    keyCompact -> "false",
    keyTree -> "true",
    keyBoundsImplicits -> "true",
    keyTruncRefined -> "0",
    keyRewrite -> "",
    keyKeepModules -> "0",
  )

  def opt(key: String, default: String) = opts.getOrElse(key, default)

  def enabled: Boolean = opt("all", "true") == "true"

  def boolean(key: String) = enabled && opt(key, "true") == "true"

  def int(key: String): Option[Int] =
    if (enabled)
      opts.get(key).flatMap(a => scala.util.Try(a.toInt).toOption)
    else
      None
}

trait Plugin
extends plugins.Plugin
with SplainPluginLike
{
  override def init(options: List[String], error: String => Unit): Boolean = {
    def invalid(opt: String) = error(s"splain: invalid option `$opt`")
    def setopt(key: String, value: String) =
      if (opts.contains(key))
        opts.update(key, value)
      else
        invalid(key)
    options.foreach { opt =>
      opt.split(":").toList match {
        case key :: value :: Nil =>
          setopt(key, value)
        case key :: Nil =>
          setopt(key, "true")
        case _ =>
          invalid(opt)
      }
    }
    enabled
  }
}

class SAnalyzer(val global: Global)
extends typechecker.Analyzer
{
  import global._

  object SLRecordItemFormatter extends SpecialFormatter {
    def keyTagName = "shapeless.labelled.KeyTag"

    def taggedName = "shapeless.tag.Tagged"

    def isKeyTag(tpe: Type) = tpe.typeSymbol.fullName == keyTagName

    def isTagged(tpe: Type) = tpe.typeSymbol.fullName == taggedName

    object extractRecord {
      def unapply(tpe: Type) =
        tpe match {
          case RefinedType(actual :: key :: Nil, _) if isKeyTag(key) =>
            Some((actual, key))
          case _ =>
            None
        }
    }

    object extractStringConstant {
      def unapply(tpe: Type) =
        tpe match {
          case ConstantType(Constant(a: String)) =>
            Some(a)
          case _ =>
            None
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

    def formatKey(tpe: Type): Formatted = formatKeyArg.lift(tpe.typeArgs).flatten.getOrElse(formatType(tpe, true))

    def recordItem(actual: Type, key: Type) = Infix(Simple("->>"), formatKey(key), formatType(actual, true), false)

    def apply[A](
      tpe: Type,
      simple: String,
      args: List[A],
      formattedArgs: => List[Formatted],
      top: Boolean,
    )(rec: (A, Boolean) => Formatted) =
      tpe match {
        case extractRecord(actual, key) =>
          Some(recordItem(actual, key))
        case _ =>
          None
      }

    def diff(left: Type, right: Type, top: Boolean) =
      (left -> right) match {
        case (extractRecord(a1, k1), extractRecord(a2, k2)) =>
          val rec: ((Formatted, Formatted), Boolean) => Formatted = {
            case ((l, r), _) =>
                if (l == r)
                  l
                else
                  Diff(l, r)
          }
          val left = formatKey(k1) -> formatKey(k2)
          val right = formatType(a1, true) -> formatType(a2, true)
          Some(formatInfix(Nil, "->>", left, right, top)(rec))
        case _ =>
          None
      }
  }

  override val specialFormatters: List[SpecialFormatter] =
    List(
      FunctionFormatter,
      TupleFormatter,
      SLRecordItemFormatter,
      RefinedFormatter,
      ByNameFormatter,
    )
}

class SplainPlugin(val global: Global)
extends Plugin
{
  import global._
  import analyzer._

  lazy val sAnalyzer: SAnalyzer = new SAnalyzer(global)

  def convertSpecifics: ImplicitErrorSpecifics => sAnalyzer.ImplicitErrorSpecifics = {
    case ImplicitErrorSpecifics.NonconformantBounds(targs, tparams, originalError) =>
      sAnalyzer.ImplicitErrorSpecifics.NonconformantBounds(
        targs.asInstanceOf[List[sAnalyzer.global.Type]],
        tparams.asInstanceOf[List[sAnalyzer.global.Symbol]],
        originalError.asInstanceOf[Option[sAnalyzer.AbsTypeError]],
      )
    case ImplicitErrorSpecifics.NotFound(param) =>
      sAnalyzer.ImplicitErrorSpecifics.NotFound(param.asInstanceOf[sAnalyzer.global.Symbol])
  }

  def convert: ImplicitError => sAnalyzer.ImplicitError = {
    case ImplicitError(tpe, candidate, nesting, specifics) =>
      sAnalyzer.ImplicitError(
        tpe.asInstanceOf[sAnalyzer.global.Type],
        candidate.asInstanceOf[sAnalyzer.global.Tree],
        nesting,
        convertSpecifics(specifics),
      )
  }

  object splainPlugin
  extends AnalyzerPlugin
  {
    override def noImplicitFoundError(param: Symbol, errors: List[ImplicitError], previous: String): String = {
      sAnalyzer.formatImplicitError(
        param.asInstanceOf[sAnalyzer.global.Symbol],
        errors.map(convert),
        "",
      )
    }
  }

  global.analyzer.addAnalyzerPlugin(splainPlugin)
}
