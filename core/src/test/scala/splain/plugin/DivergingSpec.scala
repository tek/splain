package splain.plugin

import splain.SpecBase

class DivergingSpec extends SpecBase.File {

  override protected lazy val specCompilerOptions = "-Vimplicits -Vtype-diffs"

  override lazy val defaultExtra: String = "-Vimplicits-verbose-tree -P:splain:Vimplicits-diverging"

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

  check("... without verbose-tree", "diverging-compact", extra = "-P:splain:Vimplicits-diverging") {
    checkError()
  }
}
