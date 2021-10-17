package splain.plugin

class DivergingSpecRef extends DivergingSpec {
  override lazy val suiteCanonicalName: String = classOf[DivergingSpec].getCanonicalName

  override lazy val specCompilerOptions: String = ""
}
