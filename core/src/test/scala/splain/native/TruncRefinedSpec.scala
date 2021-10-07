package splain.native

import splain.SpecBase

class TruncRefinedSpec extends SpecBase.Direct {
  override protected def extraSettings: String = "-usejavacp -Vimplicits -Vtype-diffs -Vimplicits-max-refined 5"

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

  check(truncrefined)
}
