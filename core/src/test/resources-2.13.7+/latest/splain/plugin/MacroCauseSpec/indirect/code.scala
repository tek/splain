import splain.test.AlwaysError

object MacroCause {

  trait IndirectError

  object IndirectError {

    implicit def summon(
        implicit
        ev: AlwaysError
    ): IndirectError = ???
  }

  implicitly[IndirectError]
}
