package splain.builtin

import splain.SpecBase

class BasicSpec extends SpecBase.Direct with BasicFixture {

  override protected lazy val specCompilerOptions: String = "-Vimplicits -Vtype-diffs"

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

  check(shorthandTypes, numberOfBlocks = 4)
}
