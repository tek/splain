package splain

import scala.tools.nsc._

class SplainAnalyzer(val global: Global, val pluginSettings: PluginSettings)
    extends typechecker.Analyzer
    with SplainFormattingExtension
    with ImplicitsExtension
    with SplainAnalyzerShim {

  override val specialFormatters: List[SpecialFormatter] =
    List(
      FunctionFormatter,
      TupleFormatter,
      ShapelessRecordItemFormatter,
      RefinedFormatterImproved,
//      RefinedFormatter,
      ByNameFormatter
    )
}
