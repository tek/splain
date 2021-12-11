import shapeless.Lazy

object DivergingImplicits {

  type C
  type D

  object Diverging {

    trait ::[A, B]

    implicit def f[A, B](
        implicit
        ii: Lazy[Int]
    ): A :: B = ???

    implicitly[C :: D]
  }
}
