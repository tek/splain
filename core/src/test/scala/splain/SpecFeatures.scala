package splain

import org.scalatest.Suite

import java.nio.file.{FileSystems, Files, Path}
import java.util.concurrent.atomic.AtomicInteger
import scala.tools.reflect.ToolBoxError
import scala.util.{Failure, Success, Try}

trait SpecFeatures extends Suite {

  protected def extraSettings: String = "-usejavacp -Vimplicits -Vtype-diffs"

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

    val codeWithTypes: String = SpecHelpers.types + code

    def compile(): Any = {
      import SpecHelpers._

      val tb = toolbox(extra)
      val parsed = tb.parse(codeWithTypes)
      tb.eval(parsed)
    }

    def compileError(): String =
      Try(compile()) match {
        case Failure(ee) =>
          ee match {
            case te: ToolBoxError =>
              te.message.linesIterator.toList.drop(2).mkString("\n")
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

    def checkError(errorFile: Option[String] = None) = {

      compileError() must_== groundTruth(name, errorFile)
    }

    def checkErrorWithBreak(errorFile: Option[String] = None, length: Int = 20) = {
      this.copy(extra = s"-P:splain:breakinfix:$length").checkError(errorFile)
    }

    def compileSuccess(): Option[String] =
      Try(compile()) match {
        case Success(_) =>
          None
        case Failure(t) =>
          Some(t.getMessage)
      }

    def checkSuccess() =
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
        .filter(_.nonEmpty)
        .map { line =>
          (startsWith + line).trim
        }

      result
    }

    lazy val groundTruths: Seq[String] = aggregatedGroundTruth(None)

    val pointer = new AtomicInteger(0)

    def run(code: String, extra: String = extraSettings) = {

      val cc = DirectCase(code, extra)

      cc.compileError() must_== groundTruths(pointer.getAndIncrement())
    }
  }
}
