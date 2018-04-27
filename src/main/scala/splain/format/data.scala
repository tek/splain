package splain

sealed trait Formatted
{
  def length: Int
}

case class Infix(infix: Formatted, left: Formatted, right: Formatted,
  top: Boolean)
extends Formatted
{
  def length = List(infix, left, right).map(_.length).sum + 2
}

case class Simple(tpe: String)
extends Formatted
{
  def length = tpe.length
}

case object UnitForm
extends Formatted
{
  def length = 4
}

case class Applied(cons: Formatted, args: List[Formatted])
extends Formatted
{
  def length = args.map(_.length).sum + (args.length - 1) * 2 + cons.length + 2
}

case class TupleForm(elems: List[Formatted])
extends Formatted
{
  def length = elems.map(_.length).sum + (elems.length - 1) + 2
}

case class FunctionForm(args: List[Formatted], ret: Formatted, top: Boolean)
extends Formatted
{
  def length = args.map(_.length).sum + (args.length - 1) + 2 + ret.length + 4
}

object FunctionForm
{
  def fromArgs(args: List[Formatted], top: Boolean) = {
    val (params, returnt) = args.splitAt(args.length - 1)
    FunctionForm(params, returnt.headOption.getOrElse(UnitForm), top)
  }
}

case class SLRecordItem(key: Formatted, value: Formatted)
extends Formatted
{
  def length = key.length + value.length + 5
}

case class Diff(left: Formatted, right: Formatted)
extends Formatted
{
  def length = left.length + right.length + 1
}

trait TypeRepr
{
  def broken: Boolean
  def flat: String
  def lines: List[String]
  def tokenize = lines mkString " "
  def joinLines = lines mkString "\n"
  def indent: TypeRepr
}

case class BrokenType(lines: List[String])
extends TypeRepr
{
  def broken = true
  def flat = lines mkString " "
  def indent = BrokenType(lines map ("  " + _))
}

case class FlatType(flat: String)
extends TypeRepr
{
  def broken = false
  def length = flat.length
  def lines = List(flat)
  def indent = FlatType("  " + flat)
}
