package splain.plugin

import splain.SpecBase

class DivergingSpec extends SpecBase.File {

  override lazy val specCompilerOptions: String = "-usejavacp -Vimplicits -Vimplicits-verbose-tree"

  check("self") {
    checkError()
  }

  check("circular") {
    checkError()
  }

  check("diverging") {
    checkError()
  }

//  check("divergingRef") {
//    checkError()
//  }
}
