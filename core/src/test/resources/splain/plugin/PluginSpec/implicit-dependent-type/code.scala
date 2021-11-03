import scala.annotation.implicitNotFound

object Annotation {

  trait F[I, O]

  trait G1[A, B]
  trait G2[A, B]

  implicit def f[I, M, O](
      implicit
      g1: G1[I, M],
      g2: G2[M, O]
  ): F[I, O] = ???

  implicit def g1: G1[Int, String] = ???

  implicitly[F[Int, Char]]
}
