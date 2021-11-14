package splain

import scala.tools.nsc._
import scala.tools.nsc.typechecker.MacroAnnotationNamers

class SplainPlugin(val global: Global) extends SplainPluginLike {

  //  lazy val splainAnalyzer: SplainAnalyzer = new SplainAnalyzer(global)

  lazy val pluginSettings: PluginSettings = PluginSettings(this.opts)

  lazy val splainAnalyzer: SplainAnalyzer =
    if (global.settings.YmacroAnnotations)
      new SplainAnalyzer(global, pluginSettings) with MacroAnnotationNamers
    else
      new SplainAnalyzer(global, pluginSettings)

  val analyzerField = classOf[Global].getDeclaredField("analyzer")
  analyzerField.setAccessible(true)
  analyzerField.set(global, splainAnalyzer)

  val phasesSetMapGetter = classOf[Global]
    .getDeclaredMethod("phasesSet")

  val phasesSet = phasesSetMapGetter
    .invoke(global)
    .asInstanceOf[scala.collection.mutable.Set[SubComponent]]

  if (phasesSet.exists(_.phaseName == "typer")) {
    def subcomponentNamed(name: String) =
      phasesSet
        .find(_.phaseName == name)
        .head
    val oldScs @ List(oldNamer @ _, oldPackageobjects @ _, oldTyper @ _) = List(
      subcomponentNamed("namer"),
      subcomponentNamed("packageobjects"),
      subcomponentNamed("typer")
    )
    val newScs = List(splainAnalyzer.namerFactory, splainAnalyzer.packageObjects, splainAnalyzer.typerFactory)
    phasesSet --= oldScs
    phasesSet ++= newScs
  }
  // TODO: remove them after AnalyzerPlugin interface becomes stable

  import global._
  import analyzer._

  override def init(options: List[String], error: String => Unit): Boolean = {
    def invalid(opt: String) = error(s"splain: invalid option `$opt`")
    def setOpt(key: String, value: String) =
      if (opts.contains(key))
        opts.update(key, value)
      else
        invalid(key)
    options.foreach { opt =>
      opt.split(":").toList match {
        case key :: value :: Nil =>
          setOpt(key, value)
        case key :: Nil =>
          setOpt(key, "true")
        case _ =>
          invalid(opt)
      }
    }
    pluginSettings.enabled
  }

  object SplainAnalyzerPlugin extends AnalyzerPlugin {

    protected lazy val convertSpecifics: ImplicitErrorSpecifics => splainAnalyzer.ImplicitErrorSpecifics = {
      case ImplicitErrorSpecifics.NonconformantBounds(targs, tparams, originalError) =>
        splainAnalyzer.ImplicitErrorSpecifics.NonconformantBounds(
          targs.asInstanceOf[List[splainAnalyzer.global.Type]],
          tparams.asInstanceOf[List[splainAnalyzer.global.Symbol]],
          originalError.asInstanceOf[Option[splainAnalyzer.AbsTypeError]]
        )
      case ImplicitErrorSpecifics.NotFound(param) =>
        splainAnalyzer.ImplicitErrorSpecifics.NotFound(param.asInstanceOf[splainAnalyzer.global.Symbol])
    }

    protected lazy val convertError: ImplicitError => splainAnalyzer.ImplicitError = { ee: ImplicitError =>
      import ee._

      val result = splainAnalyzer.ImplicitError(
        tpe.asInstanceOf[splainAnalyzer.global.Type],
        candidate.asInstanceOf[splainAnalyzer.global.Tree],
        nesting,
        convertSpecifics(specifics)
      )

      result
    }

    // TODO: disabled, there is no way to get annotation from any of the following argument
    override def noImplicitFoundError(param: Symbol, errors: List[ImplicitError], previous: String): String = {

      val convertedErrors = errors.map(convertError)

      if (convertedErrors == errors) {
        previous
      } else {

        val original = splainAnalyzer.formatImplicitError(
          param.asInstanceOf[splainAnalyzer.global.Symbol],
          convertedErrors,
          ""
        )

        val converted = splainAnalyzer.formatImplicitError(
          param.asInstanceOf[splainAnalyzer.global.Symbol],
          convertedErrors,
          ""
        )

        val result = previous.replace(original, converted)
        result
      }
    }
  }

  addAnalyzerPlugin(SplainAnalyzerPlugin)
}
