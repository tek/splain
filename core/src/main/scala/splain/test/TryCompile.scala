package splain.test

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.reflect.runtime.{currentMirror, universe}
import scala.tools.nsc.reporters.{Reporter, StoreReporter}
import scala.tools.nsc.{Global, Settings}
import scala.tools.reflect.ToolBox

trait TryCompile extends Product with Serializable {

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

  override lazy val toString: String = {
    s"""
       |$productPrefix
       | ---
       |${issues.mkString("\n\n")}
       |""".stripMargin
  }
}

object TryCompile {

  trait Resolved extends TryCompile

  case class Success(
      issues: Seq[Issue] = Nil
  ) extends Resolved {

    abstract class Evaluable extends Success(issues) {

      def get: Any
    }
  }

  object Empty extends Success()

  trait Failure extends Resolved

  case class TypingError(issues: Seq[Issue] = Nil) extends Failure
  case class ParsingError(issues: Seq[Issue] = Nil) extends Failure
  case class OtherFailure(e: Throwable) extends Failure {
    override def issues: Seq[Issue] = Nil
  }

  trait Engine {

    def args: String

    final def apply(code: String): TryCompile =
      try {
        doCompile(code)
      } catch {
        case e: Throwable =>
          OtherFailure(e)
      }

    def doCompile(code: String): TryCompile
  }

  val mirror: universe.Mirror = currentMirror

  case class UseReflect(args: String, sourceName: String = Issue.defaultSrcName) extends Engine {

    override def doCompile(code: String): TryCompile = {

      val frontEnd = CachingFrontEnd(sourceName)

      val toolBox: ToolBox[universe.type] = mirror.mkToolBox(frontEnd, options = args)

      def cached: Seq[Issue] = frontEnd.cached.toSeq

      val parsed =
        try {
          toolBox.parse(code.trim)
        } catch {
          case _: Throwable =>
            return TryCompile.ParsingError(cached)
        }

      val compiled =
        try {
//        toolBox.typecheck(parsed, withImplicitViewsDisabled = false)
          toolBox.compile(parsed)
        } catch {
          case _: Throwable =>
            return TryCompile.TypingError(cached)
        }

      val success = Success(cached)
      new success.Evaluable {
        override def get: Any = compiled()
      }

    }
  }

  case class UseNSC(args: String, sourceName: String = Issue.defaultSrcName) extends Engine {

    val global: Global = {
      val _settings = new Settings()

      _settings.reporter.value = classOf[StoreReporter].getCanonicalName
      _settings.usejavacp.value = true
      _settings.processArgumentString(args)

      val global: Global = Global(_settings, Reporter(_settings))
      global
    }

    val reporter: StoreReporter = global.reporter.asInstanceOf[StoreReporter]

    override def doCompile(code: String): TryCompile = reporter.synchronized { // shared reporter is not thread safe

      reporter.reset()

      val tree = global.newCompilationUnit(code.trim, sourceName)

      val run = new global.Run()

      val parser = global.newUnitParser(tree)
      parser.parse()

      def reports = reporter.infos.toSeq.map { info =>
        Issue(info.severity.id, info.msg, info.pos, sourceName)
      }

      val result = if (reports.exists(v => v.severity == Empty.Error.level)) {

        ParsingError(reports)
      } else {

        run.compileUnits(List(tree))

        val success = Success(reports)
        if (success.Error.filteredIssues.nonEmpty) {
          TypingError(reports)
        } else {
          success
        }
      }

      result
    }
  }

  case class Static[N <: String with Singleton](sourceName: N) {

    type NN = N

    def apply(code: String): TryCompile = macro TryCompileMacros.compileCodeTree[N]

    trait FromCodeMixin {

      implicit def code2TryCompile(code: String): TryCompile = macro TryCompileMacros.compileCodeTree[N]
    }
  }

  object Static {

    val default: Static["newSource1.scala"] = Static(Issue.defaultSrcName)

    def apply(): Static["newSource1.scala"] = default
  }
}
