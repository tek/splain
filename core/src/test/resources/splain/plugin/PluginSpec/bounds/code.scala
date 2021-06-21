object NonconformantBounds
{
  trait F[A]
  implicit def f[A <: C, B]: F[A] = ???
  implicitly[F[D *** C]]
}
