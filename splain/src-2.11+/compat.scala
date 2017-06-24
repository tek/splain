package splain

object OptionOps
{
  def contains[A](a: A)(o: Option[A]): Boolean = o.contains(a)
}
