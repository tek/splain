object ByName {
  type A

  def f(
      implicit
      a: => A
  ): Unit = ???

  {
    implicit val a: A = ???

    f
  }
}
