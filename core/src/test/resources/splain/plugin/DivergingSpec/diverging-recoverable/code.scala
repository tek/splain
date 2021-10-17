object DivergingImplicits {

  type C
  type D

  trait LowLevel {

    implicit def f2[A, B]: A :: B = ???
  }

  object Diverging extends LowLevel {
    trait ::[A, B]

    implicit def f[A, B](
        implicit
        ii: Int :: A :: B
    ): A :: B = ???

    implicit def g[A, B]: Int :: Int :: Int :: A :: B = ???

    implicitly[C :: D]
  }
}
