package splain.plugin

import splain.SpecBase

class PluginSpec extends SpecBase.File {

  check("implicit resolution chains", "chain") {
    checkError()
  }

  check("found/required type", "foundreq") {
    checkError()
  }

  check("bounds") {
    checkError()
  }

  check("aux type", "aux") {
    checkError()
  }

  check("lazy") {
    checkError()
  }

  check("linebreak long infix types", "break") {
    checkErrorWithBreak()
  }

  check("shapeless Record", "record") {
    checkErrorWithBreak(length = 30)
  }

  check("deephole") {
    checkError()
  }

  check("tree", extra = "-P:splain:tree") {
    checkError()
  }

  check("compact tree printing", "tree", extra = "-P:splain:tree -P:splain:compact") {
    checkError(Some("errorCompact"))
  }

  check("prefix stripping", "prefix", extra = "-P:splain:keepmodules:2") {
    checkError()
  }

  check("regex-rewrite", extra = "-P:splain:rewrite:\\.Level;0/5") {
    checkError()
  }

  check("refined type diff", "refined") {
    checkError()
  }

  check("disambiguate types", "disambiguate") {
    checkError()
  }

  check("truncate refined type", "truncrefined", extra = "-P:splain:truncrefined:10") {
    checkError()
  }

  check("byname higher order", "byname-higher") {
    checkError()
  }

  check("tuple1") {
    checkError()
  }

  check("single types", "single") {
    checkError()
  }

  check("single types in function", "single-fn") {
    checkError()
  }

  check("single types with free symbol", "single-free") {
    checkError()
  }

  check("witness value types", "witness-value") {
    checkError()
  }

  check("zlayer") {
    checkError()
  }

}
