package splain

import scala.tools.nsc._

trait Analyzer extends typechecker.Analyzer with ImplicitChains with TypeDiagnostics
