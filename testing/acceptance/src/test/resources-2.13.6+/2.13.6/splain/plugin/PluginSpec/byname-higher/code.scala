object Foo
{
  type A
  type B
  def f(g: (=> A) => B): Unit = ()
  f(1: Int)
}
