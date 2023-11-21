package splain.plugin

import splain.SpecBase

class VTypeDetailSpec extends SpecBase.Direct {

  final val wrongContexts =
    """
    object Test {

      class A {
        class B
        def b: B = new B
      }

      class F[T]

      val a = new A
      type AA = a.type

      def wrongf(a: A)(implicit b: (F[a.type])): Unit = {}

      wrongf(new A)(new F[AA])
      wrongf(new A)
    }
    """

  final val reduceToInfix =
    """
    object Test {
      trait ::[A, B]

      type K = String :: Int :: Boolean
      implicitly[K]

      def v: K = "abc"
    }
    """

  describe("#113") {

    check(wrongContexts, profile = "-P:splain:Vtype-detail:1", numberOfErrors = 2)

    check(wrongContexts, profile = "-P:splain:Vtype-detail:2", numberOfErrors = 2)

    check(wrongContexts, profile = "-P:splain:Vtype-detail:3", numberOfErrors = 2)

    check(wrongContexts, profile = "-P:splain:Vtype-detail:4", numberOfErrors = 2)

    check(wrongContexts, profile = "-P:splain:Vtype-detail:5", numberOfErrors = 2)

    check(wrongContexts, profile = "-P:splain:Vtype-detail:6", numberOfErrors = 2)
  }

  describe("#119") {

    check(reduceToInfix, profile = "-P:splain:Vtype-detail:3", numberOfErrors = 2)

    check(reduceToInfix, profile = "-P:splain:Vtype-detail:4", numberOfErrors = 2)
  }

}
