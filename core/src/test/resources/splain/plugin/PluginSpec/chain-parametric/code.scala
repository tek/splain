object Parametric {

  type F[A]
  type G[A, B]

  type C
  type D

  implicit def f[A, B](
      implicit
      g: G[A, B]
  ): F[A] = ???

  implicit def g[A](
      implicit
      h: H[A, B]
  ): G[A, List[A]] = ???

  implicitly[F[String]]
}
