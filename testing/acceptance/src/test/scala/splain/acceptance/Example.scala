package splain.acceptance

import org.scalatest.funspec.AnyFunSpec
import shapeless.test.illTyped
import splain.static.StaticAnalysis

object Example {}

class Example extends AnyFunSpec {

  final val code = """
object FoundReq {
  class L
  type R
  def f(r: R): Int = ???
  f(new L)
}"""

  it("e0") {

    illTyped(code)
  }

  it("e1") {

    val a1 = StaticAnalysis.SourceBlock.apply(
      code,
      ""
    )

    a1
  }

//  it("e2") {
//
//    val a0 = sourcecode.File()
//
//    val a1 = sourcecode.Text {
//      val a: String = 1
//    }.value
//  }

//  it("e33") {
//    val a1 = SourceBlock.apply {
//      val a: String = 1
//    }
//    a1
//  }
}
