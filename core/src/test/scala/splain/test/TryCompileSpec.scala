package splain.test

import splain.SpecBase

class TryCompileSpec extends SpecBase {

  final val code = """
object FoundReq {
  class L
  type R
  def f(r: R): Int = ???
  f(new L)
}"""

  val static = TryCompile.Static()

  describe("static") {

    it("apply") {

      val trial = static.apply(
        code
      )

      trial.toString must_==
        """
          |TypingError
          | ---
          |newSource1.scala:5: error: type mismatch;
          | found   : splain.test.TryCompileSpec.FoundReq.L
          | required: splain.test.TryCompileSpec.FoundReq.R
          |  f(new L)
          |    ^
          |""".stripMargin
    }

    it(" ... implicitly") {

      object Scope extends static.FromCodeMixin {

        val trial: TryCompile = code

        trial.toString must_==
          """
            |TypingError
            | ---
            |newSource1.scala:5: error: type mismatch;
            | found   : splain.test.TryCompileSpec.FoundReq.L
            | required: splain.test.TryCompileSpec.FoundReq.R
            |  f(new L)
            |    ^
            |""".stripMargin
      }
    }
  }

}
