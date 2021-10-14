object DivergingImplicits {

  type C
  type D

  trait LowLevel {

    implicit def f2: D = ???
  }

  object Circular extends LowLevel {
    implicit def f(
        implicit
        c: C
    ): D = ???

    implicit def g(
        implicit
        d: D
    ): C = ???

    implicitly[C]
  }
}
