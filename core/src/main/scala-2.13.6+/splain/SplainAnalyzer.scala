package splain

import scala.tools.nsc._

class SplainAnalyzer(val global: Global) extends typechecker.Analyzer with SplainFormattingExtension {

  override val specialFormatters: List[SpecialFormatter] =
    List(
      FunctionFormatter,
      TupleFormatter,
      SLRecordItemFormatter,
      ZIORefinedFormatter,
      RefinedFormatter,
      ByNameFormatter
    )

}
