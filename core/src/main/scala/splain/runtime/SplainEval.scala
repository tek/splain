package splain.runtime

import scala.reflect.runtime.{currentMirror, universe}
import scala.tools.reflect.ToolBox
import scala.util.Try

trait SplainEval {

  def settings: String

  def eval(code: String): EvalTry
}

object SplainEval {

  val mirror: universe.Mirror = currentMirror

  case class ReflectEngine(settings: String, sourceName: String = "newSource1.scala") extends SplainEval {

    override def eval(code: String): EvalTry = {

      val frontEnd = InMemoryFrontEnd(sourceName)

      val toolBox: ToolBox[universe.type] =
        ToolBox(mirror).mkToolBox(frontEnd, options = settings)

      val parsed = Try {
        toolBox.parse(code)
      }.recover {
        case e: Throwable =>
          return EvalTry.ParsingError(e.getMessage)
      }.get

      val evaled = Try {
        toolBox.eval(parsed)
      }.recover {
        case e: Throwable =>
          return EvalTry.TypeError(e.getMessage)
      }.get

      EvalTry.Success(evaled)
    }
  }
}
