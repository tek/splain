package splain.test

import splain.test.AutoLift.SerializingLift

import scala.collection.mutable.ArrayBuffer
import scala.reflect.macros.{whitebox, ParseException, TypecheckException}
import scala.tools.nsc.Global
import scala.tools.nsc.reporters.FilteringReporter

object TryCompileMacros {}

class TryCompileMacros(val c: whitebox.Context) extends SerializingLift.Mixin {
  import c.universe._

  lazy val global: Global = c.universe.asInstanceOf[Global]

  def reporter: FilteringReporter = global.reporter

  lazy val defaultSrcLit: Literal = Literal(Constant(Issue.defaultSrcName))

  def runAsMacroDefault()(code: Tree): c.universe.Tree = {

    runAsMacro(defaultSrcLit)(code)
  }

  def runAsMacro(sourceName: Tree)(code: Tree): c.universe.Tree = {
    val Literal(Constant(codeStr: String)) = code: @unchecked
    val Literal(Constant(sourceNameStr: String)) = sourceName: @unchecked

    val result: TryCompile = run(codeStr, sourceNameStr)

    q"$result"
  }

  def run(codeStr: String, sourceName: String = "macro.scala"): TryCompile = {

    val cached = ArrayBuffer.empty[Issue]

    val parsed =
      util
        .Try {
          c.parse(codeStr)
        }
        .recover {
          case e: ParseException =>
            cached += Issue(
              TryCompile.Empty.Error.level,
              e.msg,
              e.pos.asInstanceOf[scala.reflect.internal.util.Position],
              sourceName
            )

            return TryCompile.ParsingError(cached.toSeq)
        }
        .get

    val typed = util
      .Try {
        c.typecheck(parsed)
      }
      .recover {
        case e: TypecheckException =>
          cached += Issue(
            TryCompile.Empty.Error.level,
            e.msg,
            e.pos.asInstanceOf[scala.reflect.internal.util.Position],
            sourceName
          )

          return TryCompile.TypingError(cached.toSeq)
      }
      .get

    TryCompile.Success(cached.toSeq)

  }
}
