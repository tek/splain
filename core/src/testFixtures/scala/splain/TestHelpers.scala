package splain

import com.sun.org.slf4j.internal.LoggerFactory
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Assertion, Suite}
import splain.test.TryCompile

import java.nio.file.{FileSystems, Files, Path, Paths}
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Try

trait TestHelpers extends Suite {

  import TestHelpers._

  protected lazy val specCompilerOptions = "-Vimplicits -Vimplicits-verbose-tree -Vtype-diffs"

  protected lazy val defaultExtra: String = ""

  lazy val suiteCanonicalName: String = this.getClass.getCanonicalName

  final protected lazy val resourceDir: String = suiteCanonicalName.split('.').mkString("/")

  def resourcePath(name: String, fname: String): Path = FileSystems.getDefault.getPath(resourceDir, name, fname)

  def fileContentString(name: String, fname: String): String = {

    val path = resourcePath(name, fname)

    val resource = Option(
      ClassLoader.getSystemClassLoader.getResource(path.toString)
    )
      .getOrElse(throw new UnsupportedOperationException(s"Resource not found: $path"))
    val actualPath = Paths.get(resource.toURI)

    new String(Files.readAllBytes(actualPath))
  }

  def groundTruth(name: String, fname: Option[String] = None): String =
    Try {
      fileContentString(name, fname.getOrElse("error")).stripLineEnd
    }.recover { _: Throwable =>
      fileContentString(name, fname.getOrElse("check")).stripLineEnd
    }.get

  lazy val predefCode = ""

  protected lazy val effectivePredef: String = {
    val trimmed = predefCode.trim
    if (trimmed.isEmpty) {
      trimmed
    } else {
      trimmed + "\n"
    }
  }

  def getEngine(settings: String): TryCompile.Engine = {
    //    TryCompile.UseReflect(settings)
    TryCompile.UseNSC(settings)
  }

  class TestCase(code: String, extra: String) {

    lazy val codeWithPredef: String = effectivePredef + code

    case class CompileWith(settings: String) {

      private lazy val engine = getEngine(settings)

      def compile(): TryCompile = engine(codeWithPredef)

      def compileError(): String =
        compile() match {
          case v: TryCompile.TypingError =>
            v.Error.displayIssues
          case e @ _ =>
            sys.error(s"Type error not detected: $e")
        }
    }

    def compileSuccess(): Option[String] =
      splainC.compile() match {
        case _: TryCompile.Success =>
          None
        case v: TryCompile.Failure =>
          Some(v.Error.displayIssues)
      }

    def checkSuccess(): Assertion =
      assert(compileSuccess().isEmpty)

    lazy val splainC: CompileWith = CompileWith(s"$enableSplainPlugin $specCompilerOptions $extra")

    lazy val scalaC: CompileWith = CompileWith(s"$specCompilerOptions")
  }

  implicit class SpecStringOps(self: String) {

    def stripSpaceAtEnd(v: String): String = {

      v.reverse.dropWhile(v => v == ' ').reverse
    }

    def canonize(v: String): String = {
      v.split('\n').map(stripSpaceAtEnd).mkString("\n")
    }

    def must_==(groundTruth: String): Unit = {
      val left = canonize(self)
      val right = canonize(groundTruth)

      try {
        assert(left === right)
      } catch {
        case e: TestFailedException =>
          // augmenting
          val detail = {
            s"""
               |"
               |$left
               |" did not equal "
               |$right
               |"
               |""".trim.stripMargin
          }

          val ee = e.modifyMessage { _ =>
            Some(detail)
          }

          throw ee
      }
      ()
    }
  }

  case class FileCase(name: String, extra: String = defaultExtra)
      extends TestCase(fileContentString(name, "code.scala"), extra) {

    def checkError(errorFile: Option[String] = None): Unit = {

      splainC.compileError() must_== groundTruth(name, errorFile)
    }
  }

  type CheckFile = FileCase => Unit

  def checkError(errorFile: Option[String] = None): CheckFile = { cc =>
    cc.splainC.compileError() must_== groundTruth(cc.name, errorFile)
  }

  def checkErrorWithBreak(errorFile: Option[String] = None, length: Int = 20): CheckFile = { cc =>
    val withBreak = cc.copy(extra = s"-Vimplicits-breakinfix $length")
    withBreak.checkError(errorFile)
  }

  def checkSuccess(): CheckFile = { cc =>
    assert(cc.compileSuccess().isEmpty)
    ()
  }

  case class DirectCase(code: String, extra: String = defaultExtra) extends TestCase(code, extra)

  case class DirectRunner() {

    case class ParseGroundTruths(
        startsWith: String = "newSource1.scala:",
        fName: Option[String] = None
    ) {

      lazy val raw: String = {

        val gt = groundTruth("__direct", fName)
        gt
      }

      lazy val cases: Seq[String] = {
        val result = raw
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
    }

    object DefaultGroundTruths extends ParseGroundTruths()

    lazy val groundTruths: Seq[String] = DefaultGroundTruths.cases

    val pointer = new AtomicInteger(0)
  }
}

object TestHelpers {

  lazy val userDir: String = System.getProperty("user.dir").stripSuffix("/")

  val plugin: String = Option(System.getProperty("splain.jar")).getOrElse {
    val dir = FileSystems.getDefault.getPath(userDir + "/build/libs")
    val file =
      Files
        .list(dir)
        .toArray
        .map(v => v.asInstanceOf[Path])
        .filter(v => v.toString.endsWith(".jar"))
        .sortBy(v => v.toString)
        .filterNot { v =>
          Seq("-javadoc.jar", "-sources.jar", "-test-fixtures.jar")
            .map { suffix =>
              v.toString.endsWith(suffix)
            }
            .exists(identity)
        }
        .head

    LoggerFactory
      .getLogger(this.getClass)
      .debug(
        s"Using plugin jar: ${file.toString}"
      )

    file.toAbsolutePath.toString
  }

  lazy val enableSplainPlugin: String = {

    val rows = s"""
                  |-Xplugin:$plugin
                  |""".stripMargin.trim

    rows.split('\n').mkString(" ")
  }
}
