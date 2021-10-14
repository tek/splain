package splain

import scala.tools.nsc._
import scala.tools.nsc.typechecker.splain._

class SplainAnalyzer(val global: Global)
    extends typechecker.Analyzer
    with SplainFormattingExtension
    with ImplicitsExtension {

  import global._

  object SLRecordItemFormatter extends SpecialFormatter {
    def keyTagName = "shapeless.labelled.KeyTag"

    def taggedName = "shapeless.tag.Tagged"

    def isKeyTag(tpe: Type): Boolean = tpe.typeSymbol.fullName == keyTagName

    def isTagged(tpe: Type): Boolean = tpe.typeSymbol.fullName == taggedName

    object extractRecord {
      def unapply(tpe: Type): Option[(global.Type, global.Type)] =
        tpe match {
          case RefinedType(actual :: key :: Nil, _) if isKeyTag(key) =>
            Some((actual, key))
          case _ =>
            None
        }
    }

    object extractStringConstant {
      def unapply(tpe: Type): Option[String] =
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
        Some(formatType(tag, top = true))
    }

    def formatKey(tpe: Type): Formatted = formatKeyArg.lift(tpe.typeArgs).flatten.getOrElse(formatType(tpe, top = true))

    def recordItem(actual: Type, key: Type): Infix =
      Infix(Simple("->>"), formatKey(key), formatType(actual, top = true), top = false)

    def apply[A](
        tpe: Type,
        simple: String,
        args: List[A],
        formattedArgs: => List[Formatted],
        top: Boolean
    )(rec: (A, Boolean) => Formatted): Option[Formatted] =
      tpe match {
        case extractRecord(actual, key) =>
          Some(recordItem(actual, key))
        case _ =>
          None
      }

    def diff(left: Type, right: Type, top: Boolean): Option[Formatted] =
      left -> right match {
        case (extractRecord(a1, k1), extractRecord(a2, k2)) =>
          val rec: ((Formatted, Formatted), Boolean) => Formatted = {
            case ((l, r), _) =>
              if (l == r)
                l
              else
                Diff(l, r)
          }
          val left = formatKey(k1) -> formatKey(k2)
          val right = formatType(a1, top = true) -> formatType(a2, top = true)
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
      ByNameFormatter
    )

}
