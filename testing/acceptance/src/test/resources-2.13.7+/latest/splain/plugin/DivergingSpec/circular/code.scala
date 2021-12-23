object DivergingImplicits {

  type C
  type D

  object Circular {
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
