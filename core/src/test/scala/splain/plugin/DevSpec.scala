package splain.plugin

import splain.SpecBase

class DevSpec extends SpecBase.File {

  ignore("diverging") {
    checkError()
  }
}
