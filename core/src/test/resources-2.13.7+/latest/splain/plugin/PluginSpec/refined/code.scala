object Refined
{
  trait A
  trait B
  trait C
  trait D
  trait E
  trait F
  def f(a: A with B with C { type Y = String; type X = String; type Z = String }): Unit = ???
  val x: B with E with A with F { type X = Int; type Y = String } = ???
  f(x)
}
