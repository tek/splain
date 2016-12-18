package tryp

package object bp
{
  class ***[A, B]

  trait C
  trait D

  type T1 = C *** D *** (C with D { type A = D; type B = C })
  type T2 = D *** ((C *** C) *** D)
  type T3 = (D *** (C *** String)) *** (C *** D)

  implicit def e(implicit impPar2: T3): T2 = null

  implicit def d(implicit impPar1: T2): T1 = null
}

object A
{
  import bp.{C, D, T1, ***}
  val a = new (Int *** ((C *** String) *** D))
  def b(arg: Int *** ((String *** C) *** D)) = null
  def c(implicit impPar0: T1) = null
  a.attr
  b(a)
  c
}
