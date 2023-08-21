package splain.builtin

import splain.SpecBase

class MaxRefinedSpec extends SpecBase.Direct {

  def truncrefined: String = """
object TruncRefined
{
  class C
  trait D
  type CAux[A] = C { type X = C; type Y = D }
  def f(arg1: CAux[D]) = ???
  f(new D { type X = C; type Y = D })
}

  """

  check(truncrefined, nameOverride = "original")

  check(truncrefined, extra = "-Vimplicits-max-refined 5")
}
