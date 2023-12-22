package splain.plugin

import splain.SpecBase

class ZIOSpec extends SpecBase.File {

  check("zlayer") {
    // TODO: is it still incorrect?
    checkError()
  }

  check("console", profile = Profile.Splain("-Xsource:3")) {
    checkError()
  }
}
