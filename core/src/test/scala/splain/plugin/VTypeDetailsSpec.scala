package splain.plugin

import splain.SpecBase

class VTypeDetailsSpec extends SpecBase.Direct {

  final val singleType =
    """
    object SingleImp
    {
      class ***[A, B]
      val a = 1
      val b = 2

      implicitly[a.type *** b.type]
    }
"""

  check(singleType, nameOverride = "1", setting = "-P:splain:Vtype-details:1")

  check(singleType, nameOverride = "2", setting = "-P:splain:Vtype-details:2")

  check(singleType, nameOverride = "3", setting = "-P:splain:Vtype-details:3")
}
