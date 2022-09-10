package splain.test

import splain.SpecBase

object TryCompileSpec {

  final val successExample = {
    """
    object Example
    """
  }

  final val parsingErrorExample = {
    """
    object Example {
    """
  }

  class L
  type R

  final val typingErrorExample =
    """
      import splain.test.TryCompileSpec._
      
      object FoundReq {
        def f(r: R): Int = ???
        f(new L)
      }
      """

  case class S1(i: Int)

  object S1 {

    implicit def default: S1 = S1(0)
  }

  case class S2(i: Int)

  object S2Import {

    implicit def default: S2 = S2(1)
  }
}

class TryCompileSpec extends SpecBase {

  import TryCompileSpec._

  lazy val useReflect = TryCompile.UseReflect("")
  lazy val useNSC = TryCompile.UseNSC("")
  lazy val static = TryCompile.Static()

  describe("success") {

    val groundTruth = {
      """
        |Success
        | ---
        |""".stripMargin
    }

    it(classOf[TryCompile.UseReflect].getName) {
      useReflect(successExample).toString must_==
        groundTruth
    }

    it(classOf[TryCompile.UseNSC].getName) {
      useNSC(successExample).toString must_==
        groundTruth
    }

    it(classOf[TryCompile.Static[_]].getName) {
      static(successExample).toString must_==
        groundTruth
    }
  }

  describe("parsingError") {

    val groundTruth =
      """
        |ParsingError
        | ---
        |newSource1.scala:1: error: '}' expected but eof found.
        |object Example {
        |                ^
        |""".stripMargin

    it(classOf[TryCompile.UseReflect].getName) {
      useReflect(parsingErrorExample).toString must_==
        groundTruth
    }

    it(classOf[TryCompile.UseNSC].getName) {
      useNSC(parsingErrorExample).toString must_==
        groundTruth
    }

    it(classOf[TryCompile.Static[_]].getName) {
      static(parsingErrorExample).toString must_==
        groundTruth
    }
  }

  describe("typing error") {

    val groundTruth =
      """
        |TypingError
        | ---
        |newSource1.scala:5: error: type mismatch;
        | found   : splain.test.TryCompileSpec.L
        | required: splain.test.TryCompileSpec.R
        |        f(new L)
        |          ^
        |""".stripMargin

    it(classOf[TryCompile.UseReflect].getName) {
      useReflect(typingErrorExample).toString must_==
        groundTruth
    }

    it(classOf[TryCompile.UseNSC].getName) {
      useNSC(typingErrorExample).toString must_==
        groundTruth
    }

    it(classOf[TryCompile.Static[_]].getName) {
      static(typingErrorExample).toString must_==
        groundTruth
    }

    it("... invoke implicitly") {

      object Scope extends static.FromCodeMixin {

        val trial: TryCompile = typingErrorExample
      }

      Scope.trial.toString must_==
        groundTruth

    }
  }

  describe("runtime implicit search") {

    it("default scope") {
      val result = useReflect(
        """
          |implicitly[splain.test.TryCompileSpec.S1]
          |""".stripMargin
      )

      result match {
        case v: TryCompile.Success#Evaluable =>
          assert(v.issues.isEmpty)
          assert(v.get == S1(0))
        case v =>
          throw new AssertionError("Expected success\n" + v)
      }
    }

    it("imported scope") {

      val compiled = useReflect(
        """
          |import splain.test.TryCompileSpec.S2Import._
          |
          |implicitly[splain.test.TryCompileSpec.S2]
          |""".stripMargin
      )

      compiled match {
        case v: TryCompile.Success#Evaluable =>
          assert(v.issues.isEmpty)
          assert(v.get == S2(1))
        case v =>
          throw new AssertionError("Expected success\n" + v)
      }
    }
  }
}
