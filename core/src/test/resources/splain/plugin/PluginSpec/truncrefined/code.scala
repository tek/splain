object TruncRefined
{
  type CAux[A] = C { type X = C; type Y = D }
  def f(arg1: CAux[D]) = ???
  f(new D { type X = C; type Y = D })
}

