package splain.acceptance.builtin

import splain.acceptance.AcceptanceStatic

class StaticBasicSpec extends AcceptanceStatic.SpecBase {

  override lazy val suiteCanonicalName: String = "splain.builtin.BasicSpec"

  import splain.builtin.BasicFixture._

  check(chain)

  check(foundReq)

  check(foundReqSingleAbstractMethod)

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
