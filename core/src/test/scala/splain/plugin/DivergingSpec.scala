package splain.plugin

import splain.SpecBase

class DivergingSpec extends SpecBase.File {

  override lazy val specCompilerOptions: String = "-Vimplicits -Vimplicits-verbose-tree"

  check("self") {
    checkError()
  }

  check("circular") {
    checkError()
  }

  check("circular-recoverable") {
    checkError()
  }

  check("diverging") {
    checkError()
  }

  check("diverging-recoverable") {
    checkError()
  }

//  check("divergingRef") {
//    checkError()
//  }
}
