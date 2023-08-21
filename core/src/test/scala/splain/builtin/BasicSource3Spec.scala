package splain.builtin

class BasicSource3Spec extends BasicSpec {

  override lazy val suiteCanonicalName: String = classOf[BasicSpec].getCanonicalName

  override def getCompilerOptions: String = super.getCompilerOptions ++ " -Xsource:3"
}
