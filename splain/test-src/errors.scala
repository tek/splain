package splain

import java.nio.file.{Files, FileSystems}

import scala.util.{Try, Failure}
import scala.tools.reflect.{ToolBox, ToolBoxError}
import scala.reflect.runtime.universe
import scala.collection.JavaConverters._

import org.specs2._

object Helpers
{
  def base = System.getProperty("splain.tests")

  def fileContent(name: String, fname: String) = {
    FileSystems.getDefault.getPath(base, name, fname)
  }

  def fileContentString(name: String, fname: String) = {
    new String(Files.readAllBytes(fileContent(name, fname)))
  }

  def fileContentList(name: String, fname: String) = {
    Files.readAllLines(fileContent(name, fname)).asScala
  }

  def types = {
    """object types
{
  class ***[A, B]
  class >:<[A, B]
  class C
  trait D
}
import types._
"""
  }

  def code(name: String) = types + fileContentString(name, "code.scala")

  def error(name: String) = fileContentString(name, "error").stripLineEnd

  val cm = universe.runtimeMirror(getClass.getClassLoader)

  val plugin = System.getProperty("splain.jar")

  val opts = s"-Xplugin:$plugin -P:splain:color:false -P:splain:bounds"

  def toolbox(extra: String) =
    ToolBox(cm).mkToolBox(options = s"$opts $extra")
}

trait SpecBase
extends Specification
{
  import Helpers._

  def compile(name: String, extra: String) = {
    val tb = toolbox(extra)
    tb.eval(tb.parse(code(name)))
  }

  def compileError(name: String, extra: String) = {
    Try(compile(name, extra)) match {
      case Failure(ToolBoxError(e, _)) => e.lines.toList.drop(2).mkString("\n")
      case a => sys.error(s"invalid error: $a")
    }
  }

  def checkError(name: String, extra: String = "") =
    compileError(name, extra) must_== error(name)

  def checkErrorWithBreak(name: String, length: Int = 20) =
    checkError(name, s"-P:splain:breakinfix:$length")
}

class BasicSpec
extends SpecBase
{
  def is = s2"""
  implicit resolution chains ${checkError("chain")}
  ambiguous implicits ${checkError("ambiguous")}
  found/required type diff ${checkError("foundreq")}
  diff with refinement type ${checkError("refinediff")}
  nonconformant bounds ${checkError("bounds")}
  aux type ${checkError("aux")}
  shapeless Lazy ${checkError("lazy")}
  linebreak long infix types ${checkErrorWithBreak("break")}
  shapeless Record ${checkErrorWithBreak("record", 30)}
  """
}
