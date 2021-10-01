package splain.native

import org.specs2.specification.core.SpecStructure
import splain.SpecBase

class TruncRefinedSpec extends SpecBase {
  override def extraSettings: String = "-usejavacp -Vimplicits -Vtype-diffs -Vimplicits-max-refined 5"

  def code: String = ""

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

//  def show(): Unit = {
//    val global = newCompiler()
//
//    def run(code: String): Unit =
//      compileString(global)(code.trim)
//
//    run(truncrefined)
//  }

  override def is: SpecStructure = ???
}
