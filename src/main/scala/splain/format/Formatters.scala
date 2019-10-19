package splain

trait Formatters
{ self: Analyzer =>
  import global._

  def formatType(tpe: Type, top: Boolean): Formatted

  trait SpecialFormatter
  {
    def apply[A](tpe: Type, simple: String, args: List[A],
      formattedArgs: => List[Formatted], top: Boolean,
      rec: A => Boolean => Formatted): Option[Formatted]

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
}
