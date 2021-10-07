package splain

import scala.tools.nsc._

abstract class Analyzer(val global: Global) extends typechecker.Analyzer with ImplicitChains with TypeDiagnostics
