object A
{
  object B
  {
    object X
    {
      object Y
      {
        type T
      }
    }
  }
  object C
  {
    object X
    {
      object Y
      {
        type T
      }
    }
  }
  def f(a: B.X.Y.T): Unit = ()
  val x: C.X.Y.T = ???
  f(x: C.X.Y.T)
}
