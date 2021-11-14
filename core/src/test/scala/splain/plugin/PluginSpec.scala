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
      |""".trim.stripMargin

  describe("implicit resolution") {

    check("chain") {
      checkError()
    }

    check("chain-parametric") {
      checkError()
    }
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

  describe("tree printing") {
    check("tree", extra = "-Vimplicits-verbose-tree") {
      checkError()
    }

    // TODO: what's the new args?
    skip("compact", "tree", extra = "-Vimplicits-verbose-tree -P:splain:compact") {
      checkError(Some("errorCompact"))
    }
  }

  //  TODO: what's the new args?
  skip("prefix stripping", "prefix", extra = "-P:splain:keepmodules:2") {
    checkError()
  }

  //  TODO: what's the new args?
  skip("regex-rewrite", extra = "-P:splain:rewrite:\\.Level;0/5") {
    checkError()
  }

  check("refined type diff", "refined") {
    checkError()
  }

  check("disambiguate types", "disambiguate") {
    checkError()
  }

  check("truncate refined type", "truncrefined", extra = "-Vimplicits-max-refined 10") {
    checkError()
  }

  check("byname higher order", "byname-higher") {
    checkError()
  }

  check("tuple1") {
    checkError()
  }

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
