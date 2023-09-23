package splain.plugin

import splain.SpecBase

class ExplainDiffSpec extends SpecBase.Direct {

  final val diff =
    """
    object Test {
      def add2(x:Long,y:Long): Long = x + y

      def add[Long](x: List[Long], y: List[Long]): List[Long] =
        if (x.isEmpty || y.isEmpty) Nil
        else add2(x.head, y.head) :: add(x.tail, y.tail)
    }
"""

  final val diffInEq =
    """
    object Test {
      def add2[T](y: T)(
          implicit
          ev: T =:= Long
      ): Long = 1L + ev(y)

      def add[Long](x: List[Long]): List[Long] = {

        add2(x.head)
      }
    }
"""

  final val diffInSubtype =
    """
    object Test {
      def add2[T](y: T)(
          implicit
          ev: T <:< Long
      ): Long = 1L + ev(y)

      def add[Long](x: List[Long]): List[Long] = {

        add2(x.head)
      }
    }
"""

  check(diff, numberOfErrors = 2)

  check(diffInEq)

  check(diffInSubtype)
}
