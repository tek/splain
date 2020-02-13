package splain

import scala.tools.nsc._

abstract class SplainPluginCompat extends SplainPluginLike {

  trait Features {
    def featureImplicits = boolean(keyImplicits)
    def featureFoundReq = boolean(keyFoundReq)
    def featureInfix = boolean(keyInfix)
    def featureBounds = boolean(keyBounds)
    def featureColor = boolean(keyColor)
    def featureBreakInfix = int(keyBreakInfix).filterNot(_ == 0)
    def featureCompact = boolean(keyCompact)
    def featureTree = boolean(keyTree)
    def featureBoundsImplicits = boolean(keyBoundsImplicits)
    def featureTruncRefined = int(keyTruncRefined).filterNot(_ == 0)
    def featureRewrite = opt(keyRewrite, "")
    def featureKeepModules = int(keyKeepModules).getOrElse(0)
  }

  val analyzer = if (global.settings.YmacroAnnotations) {
    new { val global = SplainPluginCompat.this.global } with Analyzer with typechecker.MacroAnnotationNamers with Features
  } else {
    new { val global = SplainPluginCompat.this.global } with Analyzer with Features
  }
}
