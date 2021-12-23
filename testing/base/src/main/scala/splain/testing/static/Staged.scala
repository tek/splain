package splain.testing.static

trait Staged {}

object Staged {

  object Success extends Staged

  case class TypeError(errorMsg: String) extends Staged

  case class ParsingError(errorMsg: String) extends Staged

  case class MiscError(errorMsg: String) extends Staged
}
