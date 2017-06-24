package splain

import tools.nsc._

trait ImplicitMsgCompat
extends Formatters
{
  import analyzer._
  import global._

  def formatMsg(msg: Message, param: Symbol, tpe: Type): String =
    msg.format(TermName(param.name.toString), tpe)
}
