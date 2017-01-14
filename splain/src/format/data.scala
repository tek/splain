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
