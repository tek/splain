package splain.runtime

import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.runtime.{currentMirror, universe}
import scala.tools.nsc.reporters.{Reporter, StoreReporter}
import scala.tools.nsc.{Global, Settings}
import scala.tools.reflect.ToolBox
import scala.util.Try

trait TryCompile {

  def issues: Seq[Issue]

  case class Level(level: Int) {

    def filteredIssues: Seq[Issue] = issues.filter { i =>
      i.severity == level
    }

    def displayIssues: String = issues
      .map { i =>
        i.display
      }
      .mkString("\n")
  }

  object Error extends Level(2)
  object Warning extends Level(1)
  object Info extends Level(0)
}

object TryCompile {

  case class Unresolved(issues: Seq[Issue] = Nil) extends TryCompile {

    lazy val resolve: Resolved = {
      if (Error.filteredIssues.isEmpty) {
        Success(issues)
      } else {
        TypeError(issues)
        // TODO: how to identify ParsingError & Others
      }
    }
  }

  trait Resolved extends TryCompile

  case class Success(issues: Seq[Issue] = Nil) extends Resolved

  object Empty extends Success()

  trait Failure extends Resolved {}

  case class TypeError(issues: Seq[Issue] = Nil) extends Failure
  case class ParsingError(issues: Seq[Issue] = Nil) extends Failure
  case class OtherFailure(issues: Seq[Issue] = Nil) extends Failure

  trait Engine {

    def args: String

    def apply(code: String): TryCompile
  }

  val mirror: universe.Mirror = currentMirror

  case class UseReflect(args: String, sourceName: String = "newSource1.scala") extends Engine {

    override def apply(code: String): TryCompile = {

      val frontEnd = CachingFrontEnd(sourceName)

      val toolBox: ToolBox[universe.type] =
        ToolBox(mirror).mkToolBox(frontEnd, options = args)

      val cached = frontEnd.cached.toSeq

      val parsed = Try {
        toolBox.parse(code)
      }.recover {
        case _: Throwable =>
          return TryCompile.ParsingError(cached)
      }.get

      Try {
        toolBox.compile(parsed)
      }.recover {
        case _: Throwable =>
          return TryCompile.TypeError(cached)
      }.get

      TryCompile.Success(cached)
    }
  }

  case class UseNSC(args: String, sourceName: String = "newSource1.scala") extends Engine {

    val global: Global = {
      val _settings = new Settings()

      _settings.reporter.value = classOf[StoreReporter].getCanonicalName
      _settings.usejavacp.value = true
      _settings.processArgumentString(args)

      val global = Global(_settings, Reporter(_settings))
      global
    }

    val reporter: StoreReporter = global.reporter.asInstanceOf[StoreReporter]

    override def apply(code: String): TryCompile = {

      val run = new global.Run()

      val files = List(new BatchSourceFile(sourceName, code))

      run.compileSources(files)

      val cached = reporter.infos.toSeq.map { info =>
        Issue(info.severity.id, info.msg, info.pos, sourceName)
      }

      Unresolved(cached).resolve
    }
  }
}
