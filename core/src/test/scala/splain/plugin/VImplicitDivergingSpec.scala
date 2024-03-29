package splain.plugin

import splain.SpecBase

class VImplicitDivergingSpec extends SpecBase.File {

  override def defaultExtraSetting: String = "-Vimplicits-verbose-tree -P:splain:Vimplicits-diverging"

  check("self") {
    checkError()
  }

  check("circular") {
    checkError()
  }

  check(
    "... with max depth",
    "circular",
    profile = s"${Settings.defaultExtra} -P:splain:Vimplicits-diverging-max-depth:5"
  ) {
    checkError()
  }

  check("circular-recoverable") {
    checkSuccess()
  }

  check(
    ".... with max depth",
    "circular",
    profile = s"${Settings.defaultExtra} -P:splain:Vimplicits-diverging-max-depth:5"
  ) {
    checkError()
  }

  check("diverging") {
    checkError()
  }

  check("... without verbose-tree", "diverging-compact", profile = "-P:splain:Vimplicits-diverging") {
    checkError()
  }
}
