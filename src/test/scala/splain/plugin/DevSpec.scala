package splain.plugin

import org.specs2.specification.core.SpecStructure
import splain.SpecBase

class DevSpec extends SpecBase {
  def is = {

    val runner = FileRunner()
    import runner._

    s2"""
    diverging ${checkError("diverging")}
    """
  }
}
