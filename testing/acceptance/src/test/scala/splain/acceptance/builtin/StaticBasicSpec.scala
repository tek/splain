package splain.acceptance.builtin

import splain.acceptance.Acceptance

object StaticBasicSpec {}

class StaticBasicSpec extends Acceptance.SpecBase {

  override lazy val suiteCanonicalName: String = "splain.builtin.BasicSpec"

  import splain.builtin.BasicFixture._

  check(chain)

  check(foundReq)

  check(bounds)

  check(longAnnotationMessage)

  check(longInfix)

  check(deeplyNestedHole)

  check(auxType)

  check(refined)

  check(disambiguateQualified)

  check(bynameParam)

  check(tuple1)

  check(singleType)

  check(singleTypeInFunction)

  check(singleTypeWithFreeSymbol)

  check(parameterAnnotation)

  check(shorthandTypes)
}
