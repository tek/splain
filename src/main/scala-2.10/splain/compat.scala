package splain

import tools.nsc._, typechecker._

object OptionOps
{
  def contains[A](a: A)(o: Option[A]): Boolean = o.exists(_ == a)
}

class SplainPlugin(val global: Global)
extends Plugin
{ plugin =>
  def echo(msg: String) = ()
}
