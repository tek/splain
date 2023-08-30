package splain.builtin

import splain.SpecBase

class BasicSpec extends SpecBase.Direct with BasicFixture {

  check(chain)

  check(foundReq)

  check(foundReqVsImplicit, numberOfErrorBlocks = 0)

  check(bounds)

  check(longAnnotationMessage)

  check(longInfix)

  check(deeplyNestedHole)

  check(auxType)

  check(refined1, numberOfErrorBlocks = 2)

  check(refined2, numberOfErrorBlocks = 2)

  check(disambiguateQualified)

  check(bynameParam)

  check(tuple1)

  check(singleType)

  check(singleTypeInFunction)

  check(singleTypeWithFreeSymbol)

  check(parameterAnnotation)

  check(shorthandTypes, numberOfErrorBlocks = 4)
}
