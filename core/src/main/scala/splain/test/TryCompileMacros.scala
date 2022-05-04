package splain.test

import splain.test.AutoLift.SerializingLift

import scala.collection.mutable.ArrayBuffer
import scala.reflect.macros.{whitebox, ParseException, TypecheckException}
import scala.tools.nsc.Global
import scala.tools.nsc.reporters.FilteringReporter

class TryCompileMacros(val c: whitebox.Context) extends SerializingLift.Mixin {
  import c.universe._

  lazy val global: Global = c.universe.asInstanceOf[Global]

  def reporter: FilteringReporter = global.reporter

  lazy val defaultSrcLit: Literal = Literal(Constant(Issue.defaultSrcName))

  type CodeTree = Tree

  // TODO: from shapeless.test.IllTypedMacros, no idea what it is for
  def rectifyCode(codeStr: String): String = {

    val dummy0 = TermName(c.freshName())
    val dummy1 = TermName(c.freshName())
    s"object $dummy0 { val $dummy1 = { $codeStr } }"
  }

  final def tree2Str(code: CodeTree): String = {

    code match {
      case Literal(v) =>
        v.value.asInstanceOf[String]
      case _ =>
        throw new UnsupportedOperationException(
          s"`$code` is not a Literal, please only use Literal or final val with refined or no type annotation"
        )
    }
  }

  final def type2Str(tt: Type): String = {

    tt.dealias match {
      case v: ConstantType => v.value.value.asInstanceOf[String]
      case _ =>
        throw new UnsupportedOperationException(
          s"cannot parse type $tt : ${tt.getClass}"
        )
    }
  }

  def compileCodeTree[N <: String with Singleton: c.WeakTypeTag](code: CodeTree): Tree = {

    val _code = tree2Str(code).trim

    val _name = type2Str(implicitly[c.WeakTypeTag[N]].tpe)

    val result: TryCompile = run(_code, _name)

    q"$result"
  }

  def run(codeStr: String, sourceName: String): TryCompile = {

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

    val result = util
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
