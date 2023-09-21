package splain.plugin

import splain.SpecBase

class VTypeDiffsDetailSpec extends SpecBase.Direct {

  final val diff =
    """
    object Diff {
      def add2(x:Long,y:Long): Long = x + y

      def add[Long](x: List[Long], y: List[Long]): List[Long] =
        if (x.isEmpty || y.isEmpty) Nil
        else add2(x.head, y.head) :: add(x.tail, y.tail)
    }
    object DiffInEq {
      def add2[T](y: T)(
          implicit
          ev: T =:= Long
      ): Long = 1L + ev(y)

      def add[Long](x: List[Long]): List[Long] = {

        add2(x.head)
      }
    }
    object DiffInSubtype {
      def add2[T](y: T)(
          implicit
          ev: T <:< Long
      ): Long = 1L + ev(y)

      def add[Long](x: List[Long]): List[Long] = {

        add2(x.head)
      }
    }
"""

  check(diff, nameOverride = "1", numberOfErrors = 4, profile = "-P:splain:Vtype-diffs-detail:1")

  check(diff, nameOverride = "2", numberOfErrors = 4, profile = "-P:splain:Vtype-diffs-detail:2")

  check(diff, nameOverride = "4", numberOfErrors = 4, profile = "-P:splain:Vtype-diffs-detail:4")
}
