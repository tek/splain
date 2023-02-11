import scala.tools.nsc.typechecker.splain.SimpleName
import scala.language.implicitConversions

package object splain {

  implicit def asSimpleName(s: String): SimpleName = SimpleName(s)
}
