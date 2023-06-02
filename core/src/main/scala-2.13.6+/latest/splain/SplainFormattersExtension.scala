package splain

import scala.tools.nsc.typechecker.splain._

object SplainFormattersExtension {}

trait SplainFormattersExtension extends SplainFormatters  {
  self: SplainAnalyzer =>

  import global._

  object RefinedFormatterImproved extends SpecialFormatter {

    object DeclSymbol {
      def unapply(sym: Symbol): Option[(Formatted, Formatted)] =
        if (sym.hasRawInfo)
          Some((Simple(sym.simpleName.toString), formatType(sym.rawInfo, top = true)))
        else
          None
    }

    val ignoredTypes: List[Type] = List(typeOf[Object], typeOf[Any], typeOf[AnyRef])

    def sanitizeParents: List[Type] => List[Type] = { ps =>
      val tpes = ps.distinct
      val result = tpes.filterNot(t => ignoredTypes.exists(_ =:= t))

      if (result.isEmpty) tpes.headOption.toList
      else result
    }

    object Refined {
      def unapply(tpe: Type): Option[(List[Type], Scope)] =
        tpe match {
          case TypeRef(pre, sym, List(RefinedType(parents, decls)))
              if decls.isEmpty && pre.typeSymbol.fullName == "zio" && sym.fullName == "zio.Has" =>
            val sanitized = sanitizeParents(parents)
            if (sanitized.length == 1)
              Some((List(TypeRef(pre, sym, sanitized.headOption.toList)), decls))
            else
              None
          case RefinedType(types, scope) =>
            if (scope.isEmpty) {
              val subtypes = types.map(_.dealias).flatMap {
                case Refined(types, _) =>
                  types
                case tpe =>
                  List(tpe)
              }
              Some((subtypes, scope))
            } else
              Some((types, scope))
          case t @ SingleType(_, _) =>
            unapply(t.underlying)
          case _ =>
            None
        }
    }

    def formatDecl: Symbol => Formatted = {
      case DeclSymbol(n, t) =>
        Decl(n, t)
      case sym =>
        Simple(sym.toString)
    }

    override def apply[A](
        tpe: Type,
        simple: String,
        args: List[A],
        formattedArgs: => List[Formatted],
        top: Boolean
    )(rec: (A, Boolean) => Formatted): Option[Formatted] = {
      tpe match {
        case Refined(parents, decls) =>
          Some(RefinedForm(sanitizeParents(parents).map(formatType(_, top)), decls.toList.map(formatDecl)))
        case _ =>
          None
      }
    }

    val none: Formatted = Simple("<none>")

    def separate[A](left: List[A], right: List[A]): (List[A], List[A], List[A]) = {
      val leftS = Set(left: _*)
      val rightS = Set(right: _*)
      val common = leftS.intersect(rightS)
      val uniqueLeft = leftS -- common
      val uniqueRight = rightS -- common
      (common.toList, uniqueLeft.toList, uniqueRight.toList)
    }

    def matchTypes(left: List[Type], right: List[Type]): List[Formatted] = {
      val (common, uniqueLeft, uniqueRight) =
        separate(left.map(formatType(_, top = true)), right.map(formatType(_, top = true)))
      val diffs = uniqueLeft
        .zipAll(uniqueRight, none, none)
        .map {
          case (l, r) =>
            Diff(l, r)
        }
      common ++ diffs
    }

    def filterDecls(syms: List[Symbol]): List[(Formatted, Formatted)] =
      syms.collect {
        case DeclSymbol(sym, rhs) =>
          (sym, rhs)
      }

    def matchDecls(left: List[Symbol], right: List[Symbol]): List[Formatted] = {
      val (common, uniqueLeft, uniqueRight) = separate(filterDecls(left), filterDecls(right))
      val diffs = uniqueLeft
        .map(Some(_))
        .zipAll(uniqueRight.map(Some(_)), None, None)
        .collect {
          case (Some((sym, l)), Some((_, r))) =>
            DeclDiff(sym, l, r)
          case (None, Some((sym, r))) =>
            DeclDiff(sym, none, r)
          case (Some((sym, l)), None) =>
            DeclDiff(sym, l, none)
        }
      common.map {
        case (sym, rhs) =>
          Decl(sym, rhs)
      } ++ diffs
    }

    def diff(left: Type, right: Type, top: Boolean): Option[Formatted] =
      (left, right) match {
        case (Refined(leftParents, leftDecls), Refined(rightParents, rightDecls)) =>
          val parents = matchTypes(sanitizeParents(leftParents), sanitizeParents(rightParents)).sorted
          val decls = matchDecls(leftDecls.toList, rightDecls.toList).sorted
          Some(RefinedForm(parents, decls))
        case _ =>
          None
      }
  }

  object ShapelessRecordItemFormatter extends SpecialFormatter {
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

    override def apply[A](
        tpe: Type,
        simple: String,
        args: List[A],
        formattedArgs: => List[Formatted],
        top: Boolean
    )(rec: (A, Boolean) => Formatted): Option[Formatted] = {
      tpe match {
        case extractRecord(actual, key) =>
          Some(recordItem(actual, key))
        case _ =>
          None
      }
    }
  }
}
