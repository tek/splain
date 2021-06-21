import shapeless._
import shapeless.ops.hlist._

object SingleImp
{
  def fn[A <: AnyRef, B <: AnyRef](a: A, b: B) = {

    implicitly[a.type *** b.type]
  }
}
