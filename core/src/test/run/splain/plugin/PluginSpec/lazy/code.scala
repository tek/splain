import shapeless._
import shapeless.ops.hlist._

object LazyImp
{
  implicit def dc(implicit a: C *** D): D *** C = ???
  implicit def d(implicit a: D *** C): D = ???
  implicit def c(implicit a: Lazy[D]): C = ???
  implicitly[C]
}
