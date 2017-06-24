object Diverging
{
  implicit def f(implicit c: C): C = ???
  implicitly[C]
}
