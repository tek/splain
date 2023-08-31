import shapeless._
import shapeless.ops.hlist._

object WitnessImp {
  def fn[A, B](a: A, b: B)(
      implicit
      ev: A *** B
  ) = ???

  fn(Witness(3).value, Witness(4).value)
}
