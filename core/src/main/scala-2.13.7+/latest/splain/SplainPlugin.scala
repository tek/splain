package splain

import scala.tools.nsc._
import scala.tools.nsc.typechecker.{Analyzer, MacroAnnotationNamers}

class SplainPlugin(val global: Global) extends SplainPluginLike with PluginSettings.Implicits {

  override lazy val pluginSettings: PluginSettings = PluginSettings(this.opts)

  lazy val splainAnalyzer: SplainAnalyzer =
    if (global.settings.YmacroAnnotations.value)
      new SplainAnalyzer(global, pluginSettings) with MacroAnnotationNamers
    else
      new SplainAnalyzer(global, pluginSettings)

  {
    val analyzerField = classOf[Global].getDeclaredField("analyzer")
    analyzerField.setAccessible(true)
    val oldAnalyzer = analyzerField.get(global).asInstanceOf[Analyzer]
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

      splainAnalyzer.migrateFrom(oldAnalyzer)

      phasesSet --= oldScs
      phasesSet ++= newScs
    }
    // TODO: remove them after AnalyzerPlugin interface becomes stable
  }

  override def init(options: List[String], error: String => Unit): Boolean = {
    def invalid(opt: String): Unit = error(
      s"splain: invalid option `$opt`, supported options are ${PluginSettings.nameToKey.map(kv => "`" + kv._1 + "`").mkString(", ")}"
    )
    def setOpt(key: String, value: String): Unit =
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
    PluginSettings.Keys.enabled.get
  }
}
