package splain.plugin

import splain.SpecBase

class VTypeReductionSpec extends SpecBase.Direct {

  override protected lazy val defaultExtra: String = "-Vimplicits-max-refined 5"

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

  check(foundReqVsImplicit, extra = "-P:splain:Vtype-reduction", numberOfErrors = 2)
}
