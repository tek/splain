package splain

import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Assertion, Suite}
import org.slf4j.LoggerFactory
import splain.test.{Issue, TryCompile}

import java.nio.file.{FileSystems, Files, Path, Paths}
import java.util.concurrent.atomic.AtomicInteger
import scala.language.implicitConversions
import scala.util.Try

trait TestHelpers extends Suite {

  import TestHelpers._

  protected def basicSetting: String = "-Vimplicits -Vtype-diffs"

  protected def defaultExtraSetting: String = ""

  object Settings {

    final lazy val basic = basicSetting
    final lazy val defaultExtra: String = defaultExtraSetting
  }

  lazy val suiteCanonicalName: String = this.getClass.getCanonicalName

  final protected lazy val resourceDir: String = suiteCanonicalName.split('.').mkString("/")

  def resourcePath(name: String, fname: String): Path = FileSystems.getDefault.getPath(resourceDir, name, fname)

  def fileContentString(name: String, fname: String): String = {

    val path = resourcePath(name, fname)

    val resource = ClassLoader.getSystemClassLoader.getResource(path.toString)
    require(resource != null, s"Cannot find resource: $path")

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

  sealed trait Profile {

    def text: String
  }

  object Profile {

    case class Splain(extraSetting: String = Settings.defaultExtra) extends Profile {

      override lazy val text = s"$enableSplainPlugin $basicSetting $extraSetting"
    }

    case class BuiltIn(extraSetting: String = Settings.defaultExtra) extends Profile {

      override lazy val text = s"$basicSetting $extraSetting"
    } // use the compiler with option but no plugin

    case object Disabled extends Profile {

      override def text: String = ""
    } // just use the plain old compiler as-is

    implicit def fromString(v: String): Splain = Splain(v)

    lazy val default: Splain = Splain()
  }

  class TestCase(code: String, setting: Profile) {

    lazy val codeWithPredef: String = effectivePredef + code

    case class CompileWith(settings: String) {

      private lazy val engine = getEngine(settings)

      def compile(): TryCompile = engine(codeWithPredef)

      def compileError(): String =
        compile() match {
          case v: TryCompile.TypingError =>
            v.Error.displayIssues
          case TryCompile.OtherFailure(ee) =>
            throw new AssertionError(s"Cannot compile: $ee", ee)
          case ee @ _ =>
            throw new AssertionError(s"Type error not detected: $ee")
        }
    }

    def compileSuccess(): Option[String] =
      compileWith.compile() match {
        case _: TryCompile.Success =>
          None
        case v: TryCompile.Failure =>
          Some(v.Error.displayIssues)
      }

    def checkSuccess(): Assertion =
      assert(compileSuccess().isEmpty)

    lazy val compileWith: CompileWith = CompileWith(setting.text)
  }

  implicit class SpecStringOps(self: String) {

    def stripSpaceAtEnd(v: String): String = {

      v.reverse.dropWhile { v =>
        (v == ' ') || (v == '\r')
      }.reverse
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

          val result =
            s"""
               |expected: <
               |${right}
               |> but was: <
               |${left}
               |>
            """.stripMargin.trim

          val ee = e.modifyMessage { _ =>
            Some(result)
          }

          throw ee
      }
      ()
    }
  }

  case class FileCase(name: String, profile: Profile = Profile.default)
      extends TestCase(fileContentString(name, "code.scala"), profile) {

    def checkError(errorFile: Option[String] = None): Unit = {

      compileWith.compileError() must_== groundTruth(name, errorFile)
    }
  }

  type CheckFile = FileCase => Unit

  def checkError(errorFile: Option[String] = None): CheckFile = { cc =>
    cc.compileWith.compileError() must_== groundTruth(cc.name, errorFile)
  }

  def checkErrorWithBreak(errorFile: Option[String] = None, length: Int = 20): CheckFile = { cc =>
    val withBreak = cc.copy(profile = s"-Vimplicits-breakinfix $length")
    withBreak.checkError(errorFile)
  }

  def checkSuccess(): CheckFile = { cc =>
    assert(cc.compileSuccess().isEmpty)
    ()
  }

  case class DirectCase(code: String, setting: Profile = Profile.default) extends TestCase(code, setting)

  case class DirectRunner() {

    case class ParseGroundTruths(
        startsWith: String = Issue.defaultSrcName,
        fName: Option[String] = None
    ) {

      lazy val raw: String = {

        val gt = groundTruth("__direct", fName)
        gt
      }

      lazy val cases: Seq[String] = {
        val regex = s"(^|\n)$startsWith"

        val result = raw
          .split(
            regex
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
