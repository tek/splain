package splain.plugin

import splain.SpecBase

class PluginSpec extends SpecBase.File {

  override lazy val predefCode: String =
    """
      |object types
      |{
      |  class ***[A, B]
      |  class >:<[A, B]
      |  class C
      |  trait D
      |}
      |import types._
      |""".stripMargin.trim

  check("implicit resolution chains", "chain") {
    checkError()
  }

  check("found/required type", "foundreq") {
    checkError()
  }

  check("bounds") {
    checkError()
  }

  check("auxPattern") {
    checkError()
  }

  check("lazy") {
    checkError()
  }

  // TODO: cleanup, breakinfix is gone
  skip("linebreak long infix types", "break") {
    checkErrorWithBreak()
  }

  check("deephole") {
    checkError()
  }

  // TODO: remove, already in TreeSpec
//  describe("tree printing") {
//    check("complete", file = "tree", extra = "-Vimplicits-verbose-tree") {
//      checkError()
//    }
//
//    skip("compact", "tree", extra = "-Vimplicits-verbose-tree -P:splain:compact") {
//      checkError(Some("errorCompact"))
//    }
//  }

  //  TODO: feature removed
  skip("prefix stripping", "prefix", extra = "-P:splain:keepmodules:2") {
    checkError()
  }

  //  TODO: feature removed
  skip("regex-rewrite", extra = "-P:splain:rewrite:\\.Level;0/5") {
    checkError()
  }

  // TODO: remove, already checked in BasicSpec
//  check("refined type diff", "refined") {
//    checkError()
//  }

  check("disambiguate types", "disambiguate") {
    checkError()
  }

  // TODO: remove, already in TruncRefinedSpec
//  check("truncate refined type", "truncrefined", extra = "-Vimplicits-max-refined 10") {
//    checkError()
//  }

  // TODO: remove, already checked in BasicSpec
//  check("byname higher order", "byname-higher") {
//    checkError()
//  }

  // TODO: remove, already checked in BasicSpec
//  check("tuple1") {
//    checkError()
//  }

  describe("single types ") {

    check("single") {
      checkError()
    }

    check("in function", "single-fn") {
      checkError()
    }

    check("with free symbol", "single-free") {
      checkError()
    }
  }

  check("implicit annotation with control character(s)", "implicit-ctrl-char") {
    checkError()
  }

}
