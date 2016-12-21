package splain

package object types
{
  class ***[A, B]
  class >:<[A, B]
  class C
  trait D
}

import types._

trait Low
{
  trait I1
  trait I2
  trait I3
  trait I4
  implicit def lowI1: I1 = ???
  implicit def lowI2: I2 = ???
}

object ImplicitChain
extends Low
{
  type T1 = C *** D >:< (C with D { type A = D; type B = C })
  type T2 = D *** ((C >:< C) *** (D => Unit))
  type T3 = (D *** (C *** String)) >:< ((C, D, C) *** D)
  implicit def i1(implicit impPar7: I3): I1 = ???
  implicit def i2a(implicit impPar8: I3): I2 = ???
  implicit def i2b(implicit impPar8: I3): I2 = ???
  implicit def i4(implicit impPar9: I2): I4 = ???
  implicit def f(implicit impPar4: I4, impPar2: T3): T2 = ???
  implicit def g(implicit impPar3: I1, impPar1: T2): T1 = ???
  implicitly[T1]
}

object Ambiguous
{
  implicit val c1: C = ???
  implicit val c2: C = ???
  implicit def f1: D = ???
  implicit def f2: D = ???
  implicitly[D]
}

object NotAMember
{
  val a = new (C *** D >:< D *** C)
  a.attr
}

object FoundReq
{
  object F
  val a = new (Int *** ((C *** String) >:< D))
  def f(arg: Int *** ((String *** C { type A = F.type }) >:< D)) = ???
  f(a)
}

object RefinementDiff
{
  type CAux[A] = (C *** D) { type X = A }
  def f(arg1: CAux[D]) = ???
  f(new (C *** D) { type X = C })
}

object NonconformantBounds
{
  trait F[A]
  implicit def f[A <: C, B]: F[A] = ???
  implicitly[F[D *** C]]
}

object Aux
{
  trait F
  object F { type Aux[A] = F { type B = A } }
  implicit def f[A](implicit impPar10: C): F { type B = A } = ???
  implicitly[F.Aux[C]]
}

object Diverging
{
  implicit def f(implicit c: C): C = ???
  implicitly[C]
}
