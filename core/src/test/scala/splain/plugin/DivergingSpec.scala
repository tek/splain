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

  check("circular-recoverable") {
    checkSuccess()
  }

  check("diverging") {
    checkError()
  }
}
