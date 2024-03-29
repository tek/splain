package splain.plugin

import splain.SpecBase

class VTypeDetailReductionSpec extends SpecBase.Direct {

  final val foundReqVsImplicit =
    """
object FoundReqVsImplicit
{
trait Vec[+T] {
  type Head = Option[T]
}

val vecInt = new Vec[Int] {}
implicitly[vecInt.Head =:= Option[String]]

val x: vecInt.Head = ??? : Option[String]
}
"""

  describe("#101") {

    check(foundReqVsImplicit, numberOfErrors = 2)

    check(foundReqVsImplicit, profile = "-P:splain:Vtype-detail:reduction", numberOfErrors = 2)
  }
}
