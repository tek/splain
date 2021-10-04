import shapeless._
import shapeless.ops.hlist._

object SingleImp
{
  def fn(): Unit= {
    val a = "1"
    val b = "2"

    implicitly[a.type *** b.type]
  }
}
