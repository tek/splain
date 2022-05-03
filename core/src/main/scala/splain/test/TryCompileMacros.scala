package splain.test

import splain.test.AutoLift.SerializingLift

import scala.collection.mutable.ArrayBuffer
import scala.reflect.macros.{whitebox, ParseException, TypecheckException}
import scala.tools.nsc.Global
import scala.tools.nsc.reporters.FilteringReporter

//object TryCompileMacros {}

class TryCompileMacros(val c: whitebox.Context) extends SerializingLift.Mixin {
  import c.universe._

  lazy val global: Global = c.universe.asInstanceOf[Global]

  def reporter: FilteringReporter = global.reporter

  lazy val defaultSrcLit: Literal = Literal(Constant(Issue.defaultSrcName))

  type CodeTree = Tree

//  def summon[
//      N <: String with Singleton: c.WeakTypeTag,
//      CODE <: String with Singleton: c.WeakTypeTag
//  ]: Tree = {
//
//    println(s"compiling: ${implicitly[c.WeakTypeTag[CODE]].tpe}")
//    println(s"name: ${implicitly[c.WeakTypeTag[N]].tpe}")
//
//    val ConstantType(codeConst) = implicitly[c.WeakTypeTag[CODE]].tpe
//    val ConstantType(nameConst) = implicitly[c.WeakTypeTag[N]].tpe
//
//    val result: TryCompile = run(codeConst.value.toString, nameConst.value.toString)
//
//    val op = new StaticOp[N, CODE](result)
//
//    q"$op"
//  }

//  @tailrec
  final def expr2Str(code: CodeTree): String = {

    code match {
      case Literal(v) => v.value.asInstanceOf[String]
      case _ =>
        c.eval(c.Expr[String](c.untypecheck(code)))
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

    val _code = expr2Str(code)

    println(
      s"""
         |[CODE]
         |${_code}
         |""".stripMargin
    )

    val _name = type2Str(implicitly[c.WeakTypeTag[N]].tpe)

    val result: TryCompile = run(_code.trim, _name)

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
