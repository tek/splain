package splain.runtime

import scala.reflect.runtime.{currentMirror, universe}
import scala.tools.reflect.ToolBox
import scala.util.Try

trait TryCompile {}

object TryCompile {

  case class Success(result: Any, info: Seq[String] = Nil, warning: Seq[String] = Nil) extends TryCompile

  trait Failure extends TryCompile {
    val msg: String
  }

  case class TypeError(msg: String) extends Failure
  case class ParsingError(msg: String) extends Failure
  case class OtherFailure(msg: String) extends Failure

  trait Engine {

    def settings: String

    def apply(code: String): TryCompile
  }

  val mirror: universe.Mirror = currentMirror

  case class UseReflect(settings: String, sourceName: String = "newSource1.scala") extends Engine {

    override def apply(code: String): TryCompile = {

      val frontEnd = InMemoryFrontEnd(sourceName)

      val toolBox: ToolBox[universe.type] =
        ToolBox(mirror).mkToolBox(frontEnd, options = settings)

      val parsed = Try {
        toolBox.parse(code)
      }.recover {
        case _: Throwable =>
          return TryCompile.ParsingError(frontEnd.msg)
      }.get

      val evaled = Try {
        toolBox.eval(parsed)
      }.recover {
        case _: Throwable =>
          return TryCompile.TypeError(frontEnd.msg)
      }.get

      TryCompile.Success(evaled)
    }
  }

//  case class UseScript(settings: String, sourceName: String = "newSource1.scala") extends SplainEval {}
}
