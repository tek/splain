object DeepHole
{
  trait C1[F[_]]
  trait C2[F[_], G[_], A]
  trait C3[A, B]
  trait C4[A]
  type Id[A] = A
  type T1[X] = C3[List[String], X]
  type T2[Y] = C2[Id, C4, Y]
  type T3[Z] = C2[T1, T2, Z]
  implicitly[C1[T3]]
}
