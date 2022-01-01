package splain.acceptance

import shapeless.test.illTyped

object Example {

  val ll = List(1, 2, 3)

  illTyped(
    """
object FoundReq {
  class L
  type R
  def f(r: R): Int = ???
  f(new L)
}"""
//    """type mismatch;
//  splain.acceptance.Example.FoundReq.L|splain.acceptance.Example.FoundReq.R"""
  )
}
