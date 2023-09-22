package splain.plugin

import splain.SpecBase

class VTypeDetailsSpec extends SpecBase.Direct {

  override def defaultExtraSetting: String = Settings.defaultExtra + "-P:splain:Vtype-reduction"

  final val t7636 =
    """
    object SingleImp
    {
      object Main extends App {
        class Foo[A](x: A)
        object bar extends Foo(5: T forSome { type T })
      }
    }
  """

//  check(t7636, nameOverride = "1", profile = "-P:splain:Vtype-details:1")

//  check(t7636, nameOverride = "2", profile = "-P:splain:Vtype-details:2")
//
//  check(t7636, nameOverride = "3", profile = "-P:splain:Vtype-details:3")
//
  check(t7636, nameOverride = "4", profile = "-P:splain:Vtype-details:4")
//  check(t7636, nameOverride = "4", profile = Profile.Disabled)
}
