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
}
