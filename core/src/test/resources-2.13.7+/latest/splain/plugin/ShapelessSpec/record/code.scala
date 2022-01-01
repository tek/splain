import shapeless._
import shapeless.record._
import shapeless.syntax.singleton._

object ShapelessRecord {
  object Key
  object Value

  type Message = Record.`'sym -> String, "str" -> Value.type, Key -> Int`.T

  def show(message: Message) = ???

  val a =
    ('sym ->> "value") ::
      ("str" ->> Value) ::
      (Key ->> 42L) :: HNil

  show(a)
}
