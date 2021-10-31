package splain.plugin

import splain.SpecBase

class DivergingSpec extends SpecBase.File {

  override lazy val specCompilerOptions: String =
    "-Vimplicits -Vimplicits-verbose-tree"

  override lazy val defaultExtra: String = "-P:splain:Vimplicits-diverging"

  check("self") {
    checkError()
  }

  check("circular") {
    checkError()
  }

  check("... with max depth", "circular", extra = s"$defaultExtra -P:splain:Vimplicits-diverging-max-depth:5") {
    checkError()
  }

  check("circular-recoverable") {
    checkSuccess()
  }

  check(".... with max depth", "circular", extra = s"$defaultExtra -P:splain:Vimplicits-diverging-max-depth:5") {
    checkError()
  }

  check("diverging") {
    checkError()
  }
}
