package splain.plugin

import splain.SpecBase

class ZIOSpec extends SpecBase.File {

  check("zlayer") {
    // TODO: is it still incorrect?
    checkError()
  }

  check("source3", profile = Profile.Splain("-Xsource:3")) {
    checkError()
  }
}
