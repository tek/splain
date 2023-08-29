object Aux
{
  trait F
  object F { type Aux[A, B] = F { type X = A; type Y = B } }
  implicit def f[A, B](implicit impPar10: C): F { type X = A; type Y = B } =
    ???
  implicitly[F.Aux[C, D]]
}
