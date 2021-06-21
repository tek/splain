object DivergingImplicits {

  type C
  type D

  object Endo {
    implicit def f(
        implicit
        c: C
    ): C = ???

    implicitly[C]
  }

//  object Circular {
//    implicit def f(
//        implicit
//        c: C
//    ): D = ???
//    implicit def g(
//        implicit
//        d: D
//    ): C = ???
//
//    implicitly[C]
//  }
//
//  object Diverging {
//    trait ::[A, B]
//    implicit def f[A, B](
//        implicit
//        ii: Int :: A :: B
//    ): A :: B = ???
//
//    implicitly[C :: D]
//  }
}
