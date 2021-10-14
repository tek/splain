package splain

import org.scalatest.{Assertion, Suite}
import splain.TestHelpers.{baseOptions, cm}

import java.nio.file.{FileSystems, Files, Path}
import java.util.concurrent.atomic.AtomicInteger
import scala.reflect.internal.util.{BatchSourceFile, Position}
import scala.reflect.runtime.universe
import scala.tools.reflect.{FrontEnd, ToolBox, ToolBoxError}
import scala.util.{Failure, Success, Try}

trait TestHelpers extends Suite {

  protected lazy val specCompilerOptions = "-Vimplicits -Vimplicits-verbose-tree -Vtype-diffs"

  protected lazy val defaultExtra: String = ""

  protected def sourceName: String = "newSource1.scala"

  protected lazy val dir: String = TestHelpers.base + "/" + this.getClass.getCanonicalName.split('.').mkString("/")

  def filePath(name: String, fname: String): Path = FileSystems.getDefault.getPath(dir, name, fname)

  def fileContentString(name: String, fname: String): String = new String(Files.readAllBytes(filePath(name, fname)))

  def groundTruth(name: String, fname: Option[String]): String =
    Try {
      fileContentString(name, fname.getOrElse("error")).stripLineEnd
    }.recover { ee: Throwable =>
      fileContentString(name, fname.getOrElse("check")).stripLineEnd
    }.get

  lazy val predefCode = ""

  class TestCase(code: String, extra: String) {

    object InMemoryFrontEnd extends FrontEnd {

      @volatile var msg: String = _

      override def display(info: Info): Unit = {

        val posWithFileName = info.pos.withSource(
          new BatchSourceFile(sourceName, info.pos.source.content)
        )

        val infoStr = s"${info.severity.toString().toLowerCase}: ${info.msg}"

        val msg = Position.formatMessage(posWithFileName, infoStr, shortenFile = true)
        this.msg = msg
      }
    }

    lazy val toolbox: ToolBox[universe.type] = {

      val opt = s"$baseOptions $specCompilerOptions $extra"
      ToolBox(cm).mkToolBox(frontEnd = InMemoryFrontEnd, options = opt)
    }

    val codeWithPredef: String = (predefCode.trim + "\n" + code).trim

    def compile(): Any = {

      val parsed = toolbox.parse(codeWithPredef)
      toolbox.eval(parsed)
    }

    def compileError(): String =
      Try(compile()) match {
        case Failure(ee) =>
          ee match {
            case te: ToolBoxError =>
              InMemoryFrontEnd.msg
//              te.message.linesIterator.toList.drop(2).mkString("\n")
            case t =>
              throw t
          }
        case Success(_) =>
          sys.error("compiling succeeded")
      }
  }

  implicit class SpecStringOps(self: String) {

    def must_==(groundTruth: String): Unit = {
      assert(self == groundTruth)
    }
  }

  case class FileCase(name: String, extra: String = defaultExtra)
      extends TestCase(fileContentString(name, "code.scala"), extra) {

    def checkError(errorFile: Option[String] = None): Unit = {

      compileError() must_== groundTruth(name, errorFile)
    }

    def compileSuccess(): Option[String] =
      Try(compile()) match {
        case Success(_) =>
          None
        case Failure(t) =>
          Some(t.getMessage)
      }

    def checkSuccess(): Assertion =
      assert(compileSuccess().isEmpty)
  }

  type CheckFile = FileCase => Unit

  def checkError(errorFile: Option[String] = None): CheckFile = { cc =>
    cc.compileError() must_== groundTruth(cc.name, errorFile)
  }

  def checkErrorWithBreak(errorFile: Option[String] = None, length: Int = 20): CheckFile = { cc =>
    val withBreak = cc.copy(extra = s"-Vimplicits-breakinfix $length")
    withBreak.checkError(errorFile)
  }

  def checkSuccess(): CheckFile = { cc =>
    assert(cc.compileSuccess().isEmpty)
  }

  case class DirectCase(code: String, extra: String = defaultExtra) extends TestCase(code, extra)

  case class DirectRunner() {

    def aggregatedGroundTruth(fname: Option[String]): Seq[String] = {

      val startsWith = "newSource1.scala:"

      val gt = groundTruth("__direct", fname)

      val result = gt
        .split(
          startsWith
        )
        .toSeq
        .filter(_.nonEmpty)
        .map { line =>
          (startsWith + line).trim
        }

      result
    }

    lazy val groundTruths: Seq[String] = aggregatedGroundTruth(None)

    val pointer = new AtomicInteger(0)
  }
}

object TestHelpers {
  lazy val userDir: String = System.getProperty("user.dir").stripSuffix("/")

  lazy val base: String = {
    Option(System.getProperty("splain.tests"))
      .getOrElse(s"$userDir/src/test/resources")
  }

  val cm: universe.Mirror = universe.runtimeMirror(getClass.getClassLoader)

  val plugin: String = Option(System.getProperty("splain.jar")).getOrElse {
    val dir = FileSystems.getDefault.getPath(userDir + "/build/libs")
    val file =
      Files
        .list(dir)
        .toArray
        .map(v => v.asInstanceOf[Path])
        .filter(v => v.toString.endsWith(".jar"))
        .filterNot { v =>
          v.toString.endsWith("-javadoc.jar") ||
          v.toString.endsWith("-sources.jar")
        }
        .head

    file.toAbsolutePath.toString
  }

  lazy val baseOptions: String = {
//    val rows = s"""
//                  |-Xplugin:$plugin
//                  |-P:splain:color:false
//                  |-P:splain:bounds
//                  |-P:splain:tree:false
//                  |""".trim.stripMargin

    val rows = s"""
                  |-Xplugin:$plugin
                  |""".trim.stripMargin

    rows.split('\n').mkString(" ")
  }
}
