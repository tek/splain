package splain.acceptance

import org.scalatest.funspec.AnyFunSpec
import splain.TestHelpers
import splain.test.TryCompile

class Example extends AnyFunSpec with TestHelpers {

  it("test") {
    val result = TryCompile.Static()(
      """
    object FoundReq {
      class L
      type R
      def f(r: R): Int = ???
      f(new L)
    }
    """
    )

    result.toString must_==
      """
        |TypingError
        | ---
        |newSource1.scala:6: error: type mismatch;
        |  splain.acceptance.Example.FoundReq.L|splain.acceptance.Example.FoundReq.R
        |      f(new L)
        |        ^
        |""".stripMargin

  }
}

object Example {

  val ll = List(1, 2, 3)

//  illTyped(
//    """
//object FoundReq {
//  class L
//  type R
//  def f(r: R): Int = ???
//  f(new L)
//}"""
////    """type mismatch;
////  splain.acceptance.Example.FoundReq.L|splain.acceptance.Example.FoundReq.R"""
//  )

}
