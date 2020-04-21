package splain

trait Formatters
{ self: Analyzer =>
  import global._

  def formatType(tpe: Type, top: Boolean): Formatted

  object Refined
  {
    def unapply(tpe: Type): Option[(List[Type], Scope)] =
      tpe match {
        case RefinedType(parents, decls) =>
          Some((parents, decls))
        case t @ SingleType(_, _) =>
          unapply(t.underlying)
        case _ =>
          None
      }
  }

  trait SpecialFormatter
  {
    def apply[A](
      tpe: Type,
      simple: String,
      args: List[A],
      formattedArgs: => List[Formatted],
      top: Boolean,
      rec: A => Boolean => Formatted,
    ): Option[Formatted]

    def diff(left: Type, right: Type, top: Boolean): Option[Formatted]
  }

  object FunctionFormatter
  extends SpecialFormatter
  {
    def apply[A](tpe: Type, simple: String, args: List[A],
      formattedArgs: => List[Formatted], top: Boolean,
      rec: A => Boolean => Formatted) = {
        if (simple.startsWith("Function"))
          Some(FunctionForm.fromArgs(formattedArgs, top))
        else None
    }

    def diff(left: Type, right: Type, top: Boolean) = None
  }

  object TupleFormatter
  extends SpecialFormatter
  {
    def apply[A](tpe: Type, simple: String, args: List[A],
      formattedArgs: => List[Formatted], top: Boolean,
      rec: A => Boolean => Formatted) = {
        if (simple.startsWith("Tuple"))
          Some(TupleForm(formattedArgs))
        else None
    }

    def diff(left: Type, right: Type, top: Boolean) = None
  }

  object SLRecordItemFormatter
  extends SpecialFormatter
  {
    def keyTagName = "shapeless.labelled.KeyTag"

    def taggedName = "shapeless.tag.Tagged"

    def isKeyTag(tpe: Type) = tpe.typeSymbol.fullName == keyTagName

    def isTagged(tpe: Type) = tpe.typeSymbol.fullName == taggedName

    object extractRecord
    {
      def unapply(tpe: Type) = tpe match {
        case RefinedType(actual :: key :: Nil, _) if isKeyTag(key) =>
          Some((actual, key))
        case _ => None
      }
    }

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

    def recordItem(actual: Type, key: Type) =
      SLRecordItem(formatKey(key), formatType(actual, true))

    def apply[A](tpe: Type, simple: String, args: List[A],
      formattedArgs: => List[Formatted], top: Boolean,
      rec: A => Boolean => Formatted) = {
        tpe match {
          case extractRecord(actual, key) =>
            Some(recordItem(actual, key))
          case _ =>
            None
        }
    }

    def diff(left: Type, right: Type, top: Boolean) = {
      (left -> right) match {
        case (extractRecord(a1, k1), extractRecord(a2, k2)) =>
          val  rec = (l: Formatted, r: Formatted) => (_: Boolean) =>
            if (l == r) l else Diff(l, r)
          val recT = rec.tupled
          val left = formatKey(k1) -> formatKey(k2)
          val right = formatType(a1, true) -> formatType(a2, true)
          Some(formatInfix("->>", left, right, top, recT))
        case _ => None
      }
    }
  }

  object RefinedFormatter
  extends SpecialFormatter
  {
    object DeclSymbol
    {
      def unapply(sym: Symbol): Option[(Formatted, Formatted)] =
        if (sym.hasRawInfo) Some((Simple(sym.simpleName.toString), formatType(sym.rawInfo, true)))
        else None
    }

    val ignoredTypes: List[Type] =
      List(typeOf[Object], typeOf[Any], typeOf[AnyRef])

    def sanitizeParents: List[Type] => List[Type] = {
      case List(tpe) =>
        List(tpe)
      case tpes =>
        tpes.filterNot(t => ignoredTypes.exists(_ =:= t))
    }

    def formatDecl: Symbol => Formatted = {
      case DeclSymbol(n, t) => Decl(n, t)
      case sym => Simple(sym.toString)
    }

    def apply[A](
      tpe: Type,
      simple: String,
      args: List[A],
      formattedArgs: => List[Formatted],
      top: Boolean,
      rec: A => Boolean => Formatted
    ): Option[Formatted] =
      tpe match {
        case Refined(parents, decls) =>
          Some(RefinedForm(sanitizeParents(parents).map(formatType(_, top)), decls.toList.map(formatDecl)))
        case _ =>
          None
      }

    val none: Formatted =
      Simple("<none>")

    def separate[A](left: List[A], right: List[A])
    : (List[A], List[A], List[A]) = {
      val leftS = Set(left: _*)
      val rightS = Set(right: _*)
      val common = leftS.intersect(rightS)
      val uniqueLeft = leftS -- common
      val uniqueRight = rightS -- common
      (common.toList, uniqueLeft.toList, uniqueRight.toList)
    }

    def matchTypes(left: List[Type], right: List[Type]): List[Formatted] = {
      val (common, uniqueLeft, uniqueRight) =
        separate(left.map(formatType(_, true)), right.map(formatType(_, true)))
      val diffs =
        uniqueLeft.toList
          .zipAll(uniqueRight.toList, none, none)
          .map { case (l, r) => Diff(l, r) }
      common.toList ++ diffs
    }

    def filterDecls(syms: List[Symbol]): List[(Formatted, Formatted)] =
      syms.collect { case DeclSymbol(sym, rhs) => (sym, rhs) }

    def matchDecls(left: List[Symbol], right: List[Symbol]): List[Formatted] = {
      val (common, uniqueLeft, uniqueRight) =
        separate(filterDecls(left), filterDecls(right))
      val diffs =
        uniqueLeft.toList.map(Some(_))
          .zipAll(uniqueRight.toList.map(Some(_)), None, None)
          .collect {
            case (Some((sym, l)), Some((_, r))) => DeclDiff(sym, l, r)
            case (None, Some((sym, r))) => DeclDiff(sym, none, r)
            case (Some((sym, l)), None) => DeclDiff(sym, l, none)
          }
      common.toList.map { case (sym, rhs) => Decl(sym, rhs) } ++ diffs
    }

    def diff(left: Type, right: Type, top: Boolean): Option[Formatted] = {
      (left, right) match {
        case (Refined(leftParents, leftDecls), Refined(rightParents, rightDecls)) =>
          val parents = matchTypes(sanitizeParents(leftParents), sanitizeParents(rightParents)).sorted
          val decls = matchDecls(leftDecls.toList, rightDecls.toList).sorted
          Some(RefinedForm(parents, decls))
        case _ =>
          None
      }
    }
  }
}
