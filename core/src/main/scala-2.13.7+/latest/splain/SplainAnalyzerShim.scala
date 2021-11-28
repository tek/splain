package splain

import scala.tools.nsc.typechecker.Analyzer

trait SplainAnalyzerShim {
  self: SplainAnalyzer =>

  def migrateFrom(old: Analyzer): Unit = {

    // fix for #81: transfer deferredOpen that are cached before this initializer
    old.packageObjects.deferredOpen.foreach { v =>
      self.packageObjects.deferredOpen.add(v.asInstanceOf[self.global.Symbol])
    }
  }
}
