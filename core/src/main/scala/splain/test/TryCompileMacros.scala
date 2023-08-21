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
          s"`$code` (${code.getClass.getName}) is not a Literal, please only use Literal or final val with refined or no type annotation"
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

    val result = run(_code, _name)

    result
  }

  def run(codeStr: String, sourceName: String): Tree = {

    val cached = ArrayBuffer.empty[Issue]

    val parsed =
      try {
        c.parse(codeStr)
      } catch {
        case e: ParseException =>
          cached += Issue(
            TryCompile.Empty.Error.level,
            e.msg,
            e.pos.asInstanceOf[scala.reflect.internal.util.Position],
            sourceName
          )

          val result = TryCompile.ParsingError(cached.toSeq)

          return q"$result"
      }

    val compiled: c.Tree =
      try {
        c.typecheck(parsed)
      } catch {
        case e: TypecheckException =>
          cached += Issue(
            TryCompile.Empty.Error.level,
            e.msg,
            e.pos.asInstanceOf[scala.reflect.internal.util.Position],
            sourceName
          )
          // TODO: this can only capture the first error, which makes the result different from runtime compilation
          //   unfortunately there is nothing we can do

          val result = TryCompile.TypingError(cached.toSeq)

          return q"$result"
      }

    val success = TryCompile.Success(cached.toSeq)

//    q"$success"

    q"""
      val ss = $success

      new ss.Evaluable {
        override def get: Any = {
          $compiled
        }
      }
     """
  }
}
