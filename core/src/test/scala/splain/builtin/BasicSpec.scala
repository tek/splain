package splain.builtin

import splain.SpecBase

class BasicSpec extends SpecBase.Direct with BasicFixture {

  check(chain)

  describe("#121") {

    check(foundReq)

    check(longFoundReq)
  }

  check(longArg)

  describe("#34") {
    check(compoundDiff, numberOfErrors = 2)
  }

  describe("#111") {

    check(LongRefined)

    check(LongTuple)

    check(foundReqLongTuple)
  }

  check(foundReqSameSymbol)

  check(bounds)

  check(longAnnotationMessage)

  check(longInfix)

  check(deeplyNestedHole)

  check(auxType)

  check(refined1, numberOfErrors = 2)

  check(refined2, numberOfErrors = 2)

  check(disambiguateQualified)

  check(bynameParam)

  check(tuple1)

  check(singleType)

  check(singleTypeInFunction)

  check(singleTypeWithFreeSymbol)

  check(parameterAnnotation)

  check(shorthandTypes, numberOfErrors = 4)
}
