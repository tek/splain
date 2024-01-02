package splain.acceptance.builtin

object Example {

  object Long {
    class VeryLong[T]
    class VeryLong2[T]

    type Found = VeryLong[
      VeryLong[VeryLong[VeryLong[VeryLong[VeryLong[String]]]]]
    ]

    type Req = VeryLong[
      VeryLong[VeryLong2[VeryLong[VeryLong[VeryLong[String]]]]]
    ]

    val str: Req = ??? : Found
  }
}
