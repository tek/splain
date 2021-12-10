package splain

import scala.tools.nsc.typechecker.splain.SimpleName
import scala.language.implicitConversions

trait SplainFormattersShim {

  implicit def asSimpleName(s: String): SimpleName = SimpleName(s)
}
