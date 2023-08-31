package splain.builtin

class BasicXSource3Spec extends BasicSpec {

  override lazy val suiteCanonicalName: String = classOf[BasicSpec].getCanonicalName

  override lazy val defaultExtra: String = "-Xsource:3"
}
