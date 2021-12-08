package splain

import scala.tools.nsc._

class SplainAnalyzer(val global: Global, val pluginSettings: PluginSettings)
    extends typechecker.Analyzer
    with SplainFormattingExtension
    with ImplicitsExtension {

  override val specialFormatters: List[SpecialFormatter] =
    List(
      FunctionFormatter,
      TupleFormatter,
      SLRecordItemFormatter,
      RefinedFormatterImproved,
//      RefinedFormatter,
      ByNameFormatter
    )
}
