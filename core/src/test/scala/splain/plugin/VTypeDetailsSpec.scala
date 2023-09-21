package splain.plugin

import splain.SpecBase

class VTypeDetailsSpec extends SpecBase.Direct {

  override def defaultExtraSetting: String = Settings.defaultExtra + "-P:splain:Vtype-reduction"

  final val wrongContexts =
    """
    object Test {

      class A {
        class B
        def b: B = new B
      }

      class F[T]

      val a = new A

      def wrongf(a: A)(implicit b: (F[a.type])): Unit = {}

      wrongf(new A)(new F[a.type])
      wrongf(new A)
    }
    """

  check(wrongContexts, nameOverride = "1", profile = "-P:splain:Vtype-detail:1", numberOfErrors = 2)

  check(wrongContexts, nameOverride = "2", profile = "-P:splain:Vtype-detail:2", numberOfErrors = 2)

  check(wrongContexts, nameOverride = "3", profile = "-P:splain:Vtype-detail:3", numberOfErrors = 2)

  check(wrongContexts, nameOverride = "4", profile = "-P:splain:Vtype-detail:4", numberOfErrors = 2)
}
