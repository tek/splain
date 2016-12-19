package splain

package object bp
{
  class ***[A, B]
  class >:<[A, B]

  class C
  trait D
  trait E
  object F
  trait G[A]

  type CAux[B] = C { type A = B }

  type T1 = C *** D >:< (C with D { type A = D; type B = C })
  type T2 = D *** ((C >:< C) *** (D => Unit))
  type T3 = (D *** (C *** String)) >:< ((C, D, C) *** D)

  implicit val c1: C = ???

  implicit val c2: C = ???

  implicit def e(implicit impPar2: T3): T2 = ???

  implicit def d(implicit impPar1: T2): T1 = ???

  implicit def f1: D = ???

  implicit def f2: D = ???

  implicit def h(implicit impPar4: D): E = ???

  implicit def j[A <: String, B]: G[A] = ???

  def k[A](implicit impPar5: G[A]) = ???
}

object A
{
  import bp.{C, CAux, D, F, T1, ***, >:<, j, k}
  val a = new (Int *** ((C *** String) >:< D))
  def b(arg: Int *** ((String *** C { type A = F.type }) >:< D)) = ???
  def c(implicit impPar0: T1) = ???
  def i(arg: CAux[D]) = ???
  a.attr
  b(a)
  c
  val p1 = new C { type A = C }
  i(p1)
  k[D *** C]
}

object B {
  trait C[T]
  implicit def c[A, B](implicit tpeS: C[A], tpeT: C[B]): C[A with B] = ???
  def as[A](implicit tpe: C[A]) = ???
  as[Any]
}
