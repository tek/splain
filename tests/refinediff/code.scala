object RefinementDiff
{
  type CAux[A] = (C *** D) { type X = A }
  def f(arg1: CAux[D]) = ???
  f(new (C *** D) { type X = C })
}

