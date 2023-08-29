import shapeless.Poly1

object pol extends Poly1

object Functors {

  trait F[X[_]]

  implicitly[F[({ type λ[X] = Either[Int, X] })#λ]]
}
