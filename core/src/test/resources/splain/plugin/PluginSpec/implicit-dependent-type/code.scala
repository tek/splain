import scala.annotation.implicitNotFound

object Annotation {

  trait Arg

  trait F[A] {
    type K <: Arg
  }

  trait G[A]

  implicit def f[A](
      implicit
      ev: F[A]
  ): G[ev.K] = ???

  implicitly[G[Arg]]
}
