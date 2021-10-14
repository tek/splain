package splain.plugin

import splain.SpecBase

class ShapelessSpec extends SpecBase.File {

  override lazy val predefCode: String =
    """
      |object types
      |{
      |  class ***[A, B]
      |  class >:<[A, B]
      |  class C
      |  trait D
      |}
      |import types._
      |""".trim.stripMargin
  // in all error messages from toolbox, line number has to -8 to get the real line number

  check("shapeless Record", "record") {
    checkError()
  }

  check("witness value types", "witness-value") {
    checkError()
  }

  check("lazyImplicit") {
    checkError()
  }
}
