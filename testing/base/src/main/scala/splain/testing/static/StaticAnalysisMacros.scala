package splain.testing.static

import splain.testing.StaticAnalysis.Settings

import scala.reflect.macros.{whitebox, ParseException, TypecheckException}
import scala.tools.nsc.Global

object StaticAnalysisMacros {}

class StaticAnalysisMacros(val c: whitebox.Context) {

  import c.universe._

  {
    val casted = c.asInstanceOf[scala.reflect.macros.runtime.Context]
    val global: Global = casted.universe
    val alternative = Global(global.settings, global.reporter)
  }

  def codeBlock[TT <: Settings: WeakTypeTag](tree: c.Tree): c.Tree = try {

    println(tree)
    val codeStr = tree.toString()

    doCode(codeStr)
  } catch {
    case e: Throwable =>
      ???
  }

  def codeLiteral[TT <: Settings: WeakTypeTag](code: c.Tree): c.Tree = {

    val Literal(Constant(codeStr: String)) = code

    doCode(codeStr)
  }

  def doCode[TT <: Settings: WeakTypeTag](codeStr: String): c.Tree = {

    try {
      val dummy0 = TermName(c.freshName())
      val dummy1 = TermName(c.freshName())
      c.typecheck(c.parse(s"object $dummy0 { val $dummy1 = { $codeStr } }"))

      reify {
        Staged.SuccessBatchSourceFile
      }.tree
    } catch {
      case e: TypecheckException =>
        val msg = e.getMessage

        q"${reify(Staged.TypeError).tree}(${msg})"

      case e: ParseException =>
        val msg = e.getMessage

        q"${reify(Staged.ParsingError).tree}(${msg})"
      case e: Throwable =>
        val msg = e.getMessage

        q"${reify(Staged.MiscError).tree}(${msg})"
    }
  }
}
