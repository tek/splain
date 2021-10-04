package splain.plugin

import org.specs2.specification.core.SpecStructure
import splain.SpecBase

class ErrorsCompatSpec extends SpecBase {
  def is: SpecStructure = {

    val runner = FileRunner()
    import runner._

    s2"""
    byname ${checkSuccess("byname")}
    """
  }
}
