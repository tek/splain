import annotation.implicitNotFound

object Annotation {

  trait Arg

  @implicitNotFound("A\n   | B\n  | C")
  trait F[A]

  trait G[A]

  implicit def f[A](
      implicit
      ev: F[A]
  ): G[A] = ???

  implicitly[G[Arg]]
}
