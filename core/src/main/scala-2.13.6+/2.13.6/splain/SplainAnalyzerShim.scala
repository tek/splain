package splain

import scala.tools.nsc.typechecker.Analyzer

trait SplainAnalyzerShim {
  self: SplainAnalyzer =>

  def migrateFrom(old: Analyzer): Unit = {}
}
