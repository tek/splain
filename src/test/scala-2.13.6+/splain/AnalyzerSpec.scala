package splain

import org.specs2.Specification

import scala.tools.reflect.ToolBoxError
import scala.util.{Failure, Success, Try}

trait AnalyzerSpecBase extends Specification {
  import Helpers._

  def compile(name: String, extra: String): Any = {
    val compilerSplainOpts = " -Vimplicits -Vtype-diffs"
    val tb = toolbox(extra + compilerSplainOpts)
    tb.eval(tb.parse(code(name)))
  }

  def compileError(name: String, extra: String): String =
    Try(compile(name, extra)) match {
      case Failure(ToolBoxError(e, _)) =>
        e.linesIterator.toList.drop(2).mkString("\n")
      case Failure(t) =>
        throw t
      case Success(_) =>
        sys.error("compiling succeeded")
    }

  def checkError(name: String, extra: String = "", errorFile: Option[String] = None) =
    compileError(name, extra) must_== error(name, errorFile)

  def checkErrorWithBreak(name: String, length: Int = 20) = checkError(name, s"-P:splain:breakinfix:$length")

  def compileSuccess(name: String, extra: String): Option[String] =
    Try(compile(name, extra)) match {
      case Success(_) =>
        None
      case Failure(t) =>
        Some(t.getMessage)
    }

  def checkSuccess(name: String, extra: String = "") = compileSuccess(name, extra) must beNone
}

class AnalyzerSpec extends AnalyzerSpecBase {

  def is =
    s2"""
    zio test ${checkError("zlayer")}
    """
}
