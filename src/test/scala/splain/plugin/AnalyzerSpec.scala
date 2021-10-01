package splain.plugin

import org.specs2.specification.core.SpecStructure
import splain.SpecBase

class AnalyzerSpec extends SpecBase {

  def is: SpecStructure = {

    val runner = FileRunner()
    import runner._

    s2"""
    zio test ${checkError("zlayer")}
    """
  }
}
