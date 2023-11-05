package splain

import scala.collection.mutable
import scala.tools.nsc._

class SplainAnalyzer(val global: Global, val pluginSettings: PluginSettings)
    extends typechecker.Analyzer
    with SplainFormattingExtension
    with ImplicitsExtension
    with SplainAnalyzerShim
    with PluginSettings.Implicits {

  override val specialFormatters: List[SpecialFormatter] =
    List(
      FunctionFormatter,
      TupleFormatter,
      ShapelessRecordItemFormatter,
      RefinedFormatterImproved,
//      RefinedFormatter,
      ByNameFormatter
    )

  override def splainFoundReqMsg(found: global.Type, req: global.Type): String = {
    val original = super.splainFoundReqMsg(found, req)

    val extra = mutable.Buffer.empty[String]

    if (PluginSettings.Keys.debug.isEnabled) {

      extra += "===[ ORIGINAL ERROR ]===" +
        builtinFoundReqMsg(found, req) +
        "\n"
    }

    val result = (Seq(original) ++ extra.toSeq).mkString("\n")
    result
  }
}
