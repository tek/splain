package splain

import java.nio.file.{Files, FileSystems}

import scala.util.{Try, Failure}
import scala.tools.reflect.{ToolBox, ToolBoxError}
import scala.reflect.runtime.universe
import scala.collection.JavaConversions._

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
    Files.readAllLines(fileContent(name, fname)).toList
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

  def error(name: String) = fileContentList(name, "error")

  val cm = universe.runtimeMirror(getClass.getClassLoader)

  val plugin = System.getProperty("splain.jar")

  val opts = s"-Xplugin:$plugin -P:splain:color:false -P:splain:bounds"

  val tb =
    ToolBox(cm).mkToolBox(options = opts)
}

trait SpecBase
extends Specification
{
  import Helpers._

  def compileError(name: String) =
    Try(tb.eval(tb.parse(code(name)))) match {
      case Failure(ToolBoxError(e, _)) => e.lines.toList.drop(2).map(_.trim)
      case a => sys.error(s"invalid error: $a")
    }

  def checkError(name: String) = compileError(name) must_== error(name)
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
  """
}
