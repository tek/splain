object DivergingImplicits {

  type C
  type D

  object Endo {
    implicit def f(
        implicit
        d: D
    ): C = ???

    implicitly[C]
  }
}
