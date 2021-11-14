package splain

import scala.tools.nsc.typechecker.splain.SimpleName
import scala.language.implicitConversions

trait FormattedNameShim {

  implicit def asSimpleName(s: String): SimpleName = SimpleName(s)
}
