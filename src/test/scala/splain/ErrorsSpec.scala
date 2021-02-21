package splain

import java.nio.file.{FileSystems, Files, Path}

import scala.reflect.runtime.universe
import scala.tools.reflect.{ToolBox, ToolBoxError}
import scala.util.{Failure, Success, Try}

import org.specs2._

object Helpers {
  lazy val userDir = System.getProperty("user.dir").stripSuffix("/")

  def base = Option(System.getProperty("splain.tests"))
    .getOrElse(userDir + "/" + "tests")

  def fileContent(name: String, fname: String): Path = {
    FileSystems.getDefault.getPath(base, name, fname)
  }

  def fileContentString(name: String, fname: String): String = {
    new String(Files.readAllBytes(fileContent(name, fname)))
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

  def error(name: String, fname: Option[String]) = fileContentString(name, fname.getOrElse("error")).stripLineEnd

  val cm = universe.runtimeMirror(getClass.getClassLoader)

  val plugin = Option(System.getProperty("splain.jar")).getOrElse {
    val dir = FileSystems.getDefault.getPath(userDir + "/target/scala-2.13")
    val file = Files
      .list(dir)
      .toArray
      .map(v => v.asInstanceOf[Path])
      .filter(v => v.toString.endsWith(".jar"))
      .head

    file.toAbsolutePath.toString
  }

  val opts = s"-Xplugin:$plugin -P:splain:color:false -P:splain:bounds -P:splain:tree:false"

  def toolbox(extra: String) =
    ToolBox(cm).mkToolBox(options = s"$opts $extra")
}

trait SpecBase extends Specification {
  import Helpers._

  def compile(name: String, extra: String) = {
    val tb = toolbox(extra)
    tb.eval(tb.parse(code(name)))
  }

  def compileError(name: String, extra: String): String = {
    Try(compile(name, extra)) match {
      case Failure(ToolBoxError(e, _)) => e.linesIterator.toList.drop(2).mkString("\n")
      case Failure(t)                  => throw t
      case Success(_)                  => sys.error("compiling succeeded")
    }
  }

  def checkError(name: String, extra: String = "", errorFile: Option[String] = None) =
    compileError(name, extra) must_== error(name, errorFile)

  def checkErrorWithBreak(name: String, length: Int = 20) =
    checkError(name, s"-P:splain:breakinfix:$length")

  def compileSuccess(name: String, extra: String): Option[String] = {
    Try(compile(name, extra)) match {
      case Success(_) => None
      case Failure(t) => Some(t.getMessage)
    }
  }

  def checkSuccess(name: String, extra: String = "") =
    compileSuccess(name, extra) must beNone
}

class ErrorsSpec extends SpecBase {

  def is = s2"""
  implicit resolution chains ${checkError("chain")}
  found/required type diff ${checkError("foundreq")}
  nonconformant bounds ${checkError("bounds")}
  aux type ${checkError("aux")}
  shapeless Lazy ${checkError("lazy")}
  linebreak long infix types ${checkErrorWithBreak("break")}
  shapeless Record ${checkErrorWithBreak("record", 30)}
  deep hole ${checkError("deephole")}
  tree printing ${checkError("tree", "-P:splain:tree")}
  compact tree printing ${checkError("tree", "-P:splain:tree -P:splain:compact", Some("errorCompact"))}
  type prefix stripping ${checkError("prefix", "-P:splain:keepmodules:2")}
  regex type rewriting ${checkError("regex-rewrite", "-P:splain:rewrite:\\.Level;0/5")}
  refined type diff ${checkError("refined")}
  disambiguate types ${checkError("disambiguate")}
  truncate refined type ${checkError("truncrefined", "-P:splain:truncrefined:10")}
  byname higher order ${checkError("byname-higher")}
  Tuple1 ${checkError("tuple1")}
  single types ${checkError("single")}
  single types in function ${checkError("single-fn")}
  single types with free symbol ${checkError("single-free")}
  witness value types ${checkError("witness-value")}
  zio test ${checkError("zlayer")}
  """

//  def is = s2"""
//  single types ${checkError("witness-value")}
//  """
}

class DevSpec extends SpecBase {
  def is = s2"""
  """
}
