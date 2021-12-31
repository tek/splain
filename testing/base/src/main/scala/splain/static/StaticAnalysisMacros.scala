package splain.static

import splain.runtime.{Issue, TryCompile}

import scala.reflect.macros.runtime.Context
import scala.reflect.macros.{whitebox, ParseException, TypecheckException}

object StaticAnalysisMacros {}

class StaticAnalysisMacros(val c: whitebox.Context) {

  import c.universe._

  lazy val macroC: Context = c.asInstanceOf[scala.reflect.macros.runtime.Context]

//  {
//    val casted = c.asInstanceOf[scala.reflect.macros.runtime.Context]
//    val global: Global = casted.universe
//    val alternative = Global(global.settings, global.reporter)
//  }

  def codeBlock[ARGS <: String: WeakTypeTag](tree: c.Tree, args: c.Tree): c.Tree = try {

    println(tree)
    val codeStr = tree.toString()

    doCompile(codeStr, args)
  } catch {
    case _: Throwable =>
      ???
  }

  def codeLiteral[ARGS <: String: WeakTypeTag](lit: c.Tree, args: c.Tree): c.Tree = {

    val Literal(Constant(codeStr: String)) = lit

    doCompile(codeStr, args)
  }

  def doCompile[ARGS <: String: WeakTypeTag](codeStr: String, args: c.Tree): c.Tree = {

    type InternalPosition = scala.reflect.internal.util.Position

    try {
      val dummy0 = TermName(c.freshName())
      val dummy1 = TermName(c.freshName())
      val checked = c.typecheck(c.parse(s"object $dummy0 { val $dummy1 = { $codeStr } }"))

      q"${reify(TryCompile.Success).tree}(${reify(Nil)})"

      // TODO: how to get warning & info?
    } catch {

      case e: TypecheckException =>
        q"${reify(TryCompile.TypeError).tree}(${reify(
          Seq(
            Issue(2, e.getMessage, e.pos.asInstanceOf[InternalPosition])
          )
        )})"
      case e: ParseException =>
        q"${reify(TryCompile.ParsingError).tree}(${reify(
          Seq(
            Issue(2, e.getMessage, e.pos.asInstanceOf[InternalPosition])
          )
        )})"
      case e: Throwable =>
        q"${reify(TryCompile.OtherFailure).tree}(${reify(
          Seq(
            Issue(2, e.getMessage, scala.reflect.internal.util.NoPosition)
          )
        )})"
    }
  }
}
