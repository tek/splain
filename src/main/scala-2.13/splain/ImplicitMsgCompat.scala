package splain

trait ImplicitMsgCompat
extends Formatters
{ self: Analyzer =>
  import global._

  def formatMsg(msg: Message, param: Symbol, tpe: Type): String =
    param match {
      case _ => msg.formatDefSiteMessage(tpe)
    }
}
