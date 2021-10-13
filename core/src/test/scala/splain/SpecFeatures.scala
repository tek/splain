package splain

import org.scalatest.{Assertion, Suite}
import splain.SpecHelpers.{cm, opts}

import java.nio.file.{FileSystems, Files, Path}
import java.util.concurrent.atomic.AtomicInteger
import scala.reflect.internal.util.{BatchSourceFile, Position}
import scala.reflect.runtime.universe
import scala.tools.reflect.{FrontEnd, ToolBox, ToolBoxError}
import scala.util.{Failure, Success, Try}

trait SpecFeatures extends Suite {

  protected def extraSettings: String = "-usejavacp -Vimplicits -Vtype-diffs"

  protected def sourceName: String = "newSource1.scala"

  protected lazy val dir: String = SpecHelpers.base + "/" + this.getClass.getCanonicalName.split('.').mkString("/")

  def filePath(name: String, fname: String): Path = FileSystems.getDefault.getPath(dir, name, fname)

  def fileContentString(name: String, fname: String): String = new String(Files.readAllBytes(filePath(name, fname)))

  def groundTruth(name: String, fname: Option[String]): String =
    Try {
      fileContentString(name, fname.getOrElse("error")).stripLineEnd
    }.recover { ee: Throwable =>
      fileContentString(name, fname.getOrElse("check")).stripLineEnd
    }.get

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

      ToolBox(cm).mkToolBox(frontEnd = InMemoryFrontEnd, options = s"$opts $extra")
      //    ToolBox(cm).mkToolBox(options = s"$opts $extra")
    }

//    lazy val predefCode: String =
//      """
//        |object types
//        |{
//        |  class ***[A, B]
//        |  class >:<[A, B]
//        |  class C
//        |  trait D
//        |}
//        |import types._
//        |""".trim.stripMargin
// in all error messages from toolbox, line number has to -8 to get the real line number
    lazy val predefCode = ""

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

  case class FileCase(name: String, extra: String = extraSettings)
      extends TestCase(fileContentString(name, "code.scala"), extra) {

    def checkError(errorFile: Option[String] = None): Unit = {

      compileError() must_== groundTruth(name, errorFile)
    }

    def checkErrorWithBreak(errorFile: Option[String] = None, length: Int = 20): Unit = {
      this.copy(extra = s"-P:splain:breakinfix:$length").checkError(errorFile)
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
    cc.copy(extra = s"-P:splain:breakinfix:$length").checkError(errorFile)
  }

  def checkSuccess(): CheckFile = { cc =>
    assert(cc.compileSuccess().isEmpty)
  }

  //    def compileSuccess(): Option[String] =
  //      Try(compile()) match {
  //        case Success(_) =>
  //          None
  //        case Failure(t) =>
  //          Some(t.getMessage)
  //      }

//  case class FileRunner() {
//
//    def checkSuccess(
//        file: String,
//        extra: String = extraSettings
//    ) = FileCase(file, extra).checkSuccess()
//
//    def checkError(
//        file: String,
//        extra: String = extraSettings,
//        errorFile: Option[String] = None
//    ) = {
//
//      FileCase(file, extra).checkError(errorFile)
//    }
//
//    def checkErrorWithBreak(
//        file: String,
//        extra: String = extraSettings,
//        errorFile: Option[String] = None,
//        length: Int = 20
//    ) = {
//      FileCase(file, extra).checkErrorWithBreak(errorFile, length)
//    }
//  }

  case class DirectCase(code: String, extra: String = extraSettings) extends TestCase(code, extra)

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
