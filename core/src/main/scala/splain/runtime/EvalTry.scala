package splain.runtime

trait EvalTry {}

object EvalTry {

  case class Success(result: Any, info: Seq[String] = Nil, warning: Seq[String] = Nil) extends EvalTry

  trait Failure extends EvalTry {
    val msg: String
  }

  case class TypeError(msg: String) extends Failure

  case class ParsingError(msg: String) extends Failure

  case class OtherFailure(msg: String) extends Failure
}
