package splain.builtin

object BasicFixture extends BasicFixture

trait BasicFixture {

  // from scalac tests START HERE
  final val chain =
    """
object ImplicitChain
{
  trait I1
  trait I2
  trait I3
  trait I4
  trait II
  implicit def i1(implicit impPar7: I3): I1 = ???
  implicit def i2a(implicit impPar8: I3): I2 = ???
  implicit def i2b(implicit impPar8: I3): I2 = ???
  implicit def i4(implicit impPar9: I2): I4 = ???
  implicit def g(implicit impPar3: I1, impPar1: I4): II = ???
  implicitly[II]
}
  """

  final val foundReq =
    """
object FoundReq
{
  class L
  type R
  def f(r: R): Int = ???
  f(new L)
}
  """

  final val foundReqLongTuple =
    """
object FoundReqLong {
  class VeryLong[T]

  val x: (
      VeryLong[Int],
      VeryLong[
        VeryLong[VeryLong[VeryLong[VeryLong[VeryLong[VeryLong[VeryLong[Int]]]]]]]
      ]
  ) = ???

  val y: (
      VeryLong[Int],
      VeryLong[
        VeryLong[VeryLong[VeryLong[VeryLong[VeryLong[VeryLong[VeryLong[String]]]]]]]
      ]
  ) = x
}
"""

  final val foundReqSameSymbol =
    """
object FoundReqSameSymbol {

  trait T { type TT }
  trait A { val t: T }

  trait B {
    val a: A
    final val t = a.t

    val t1: a.t.TT = ???
    val t2: t.TT = t1
  }
}
  """

  final val bounds =
    """
object Bounds
{
  trait Base
  trait Arg
  trait F[A]
  implicit def g[A <: Base, B]: F[A] = ???
  implicitly[F[Arg]]
}
  """

  final val longAnnotationMessage =
    """
object Long
{
  def long(implicit ec: scala.concurrent.ExecutionContext): Unit = ???
  long
}
  """

  final val longInfix =
    """
object InfixBreak
{
  type ::::[A, B]
  trait VeryLongTypeName
  trait Short
  type T1 = VeryLongTypeName :::: VeryLongTypeName :::: VeryLongTypeName ::::
    VeryLongTypeName
  type T2 = T1 :::: (Short :::: Short) :::: T1 :::: T1
  implicit def f(implicit impPar4: List[T2]): String = ???
  implicitly[String]
}
  """

  final val deeplyNestedHole =
    """
object DeepHole
{
  trait C1[F[_]]
  trait C2[F[_], G[_], A]
  trait C3[A, B]
  trait C4[A]
  type Id[A] = A
  type T1[X] = C3[List[String], X]
  type T2[Y] = C2[Id, C4, Y]
  type T3[Z] = C2[T1, T2, Z]
  implicitly[C1[T3]]
}
  """

  final val auxType =
    """
object Aux
{
  trait C
  trait D
  trait F
  object F { type Aux[A, B] = F { type X = A; type Y = B } }
  implicit def f[A, B](implicit impPar10: C): F { type X = A; type Y = B } =
    ???
  implicitly[F.Aux[C, D]]
}
  """

  final val refined1 =
    """
object Refined
{
  trait A
  trait B
  trait C
  trait D
  trait E
  trait F
  def f(a: A with B with C { type Y = String; type X = String; type Z = String }): Unit = ???
  val x: B with E with A with F { type X = Int; type Y = String } = ???
  f(x)

  object Sub1 {
    trait A

    object Sub2 {
      trait B

      def f(a: A with B with C { type Y = String; type X = String; type Z = String }): Unit = ???
      val x: B with E with A with F { type X = Int; type Y = String } = ???
      f(x)
    }
  }
}
  """

  final val refined2 =
    """
object Refined
{
  trait Node {
    type T
  }

  type NodeAux[T0] = Node { type T = T0 }
  type NodeLt[T0] = NodeAux[_ <: T0]

  implicitly[NodeLt[Int]]

  val k: NodeLt[Int] = 1
}
    """

  final val disambiguateQualified =
    """
object A
{
  object B
  {
    object X
    {
      object Y
      {
        type T
      }
    }
  }
  object C
  {
    object X
    {
      object Y
      {
        type T
      }
    }
  }
  def f(a: B.X.Y.T): Unit = ()
  val x: C.X.Y.T = ???
  f(x: C.X.Y.T)
}
  """

  final val bynameParam =
    """
object Foo
{
  type A
  type B
  def f(g: (=> A) => B): Unit = ()
  f(1: Int)
}
  """

  final val tuple1 =
    """
object Tup1
{
  val a: Tuple1[String] = "Tuple1": String
}
  """

  final val singleType =
    """
object SingleImp
{
  class ***[A, B]
  val a = 1
  val b = 2

  implicitly[a.type *** b.type]
}
  """

  final val singleTypeInFunction =
    """
object SingleImp
{
  class ***[A, B]
  def fn(): Unit = {
    val a = 1
    val b = 2

    implicitly[a.type *** b.type]
  }
}
  """

  final val singleTypeWithFreeSymbol =
    """
object SingleImp
{
  class ***[A, B]
  def fn[A, B](a: A, b: B) = {

    implicitly[a.type *** b.type]
  }
}
  """

  final val parameterAnnotation =
    """
  import scala.collection.mutable
  object Test {
    val o = new Object
    val ms = scala.collection.mutable.SortedSet(1,2,3)
    ms.map(_ => o)
  }
  """
  // from scalac tests END HERE

  final val shorthandTypes =
    """
object a {
  type TypeA
  object b {
    type TypeB
    object c {
      type TypeC
      object d {
        type TypeD
        implicitly[List[TypeA]]
        implicitly[Seq[TypeB]]
        implicitly[Traversable[TypeC]]
        implicitly[Iterator[TypeD]]
      }
    }
  }
}
"""

}
