package splain.plugin

import splain.SpecBase

class VTypeDefPositionSpec extends SpecBase.Direct {

  final val diff =
    """
object Diff {
  class Example {

    type VV
  }

  val e1 = new Example {

    type VV <: Int
  }

  implicitly[e1.VV =:= String]
  val x: e1.VV = ??? : String
}
  """

  describe("#44") {

    check(diff, numberOfErrors = 2)

    check(diff, profile = "-P:splain:Vtype-def-position", numberOfErrors = 2)
  }
}
