package splain.plugin

import splain.SpecBase

class VTypeReductionSpec extends SpecBase.Direct {

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

  check(foundReqVsImplicit, nameOverride = "original", numberOfErrors = 2)

  check(foundReqVsImplicit, setting = "-P:splain:Vtype-reduction", numberOfErrors = 2)
}
