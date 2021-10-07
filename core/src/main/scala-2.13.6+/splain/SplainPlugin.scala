package splain

import scala.tools.nsc._
import scala.tools.nsc.typechecker.MacroAnnotationNamers

class SplainPlugin(val global: Global) extends SplainPluginLike {

  //  lazy val splainAnalyzer: SplainAnalyzer = new SplainAnalyzer(global)

  lazy val splainAnalyzer: SplainAnalyzer =
    if (global.settings.YmacroAnnotations)
      new SplainAnalyzer(global) with MacroAnnotationNamers
    else
      new SplainAnalyzer(global)

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
    enabled
  }

  def convertSpecifics: ImplicitErrorSpecifics => splainAnalyzer.ImplicitErrorSpecifics = {
    case ImplicitErrorSpecifics.NonconformantBounds(targs, tparams, originalError) =>
      splainAnalyzer.ImplicitErrorSpecifics.NonconformantBounds(
        targs.asInstanceOf[List[splainAnalyzer.global.Type]],
        tparams.asInstanceOf[List[splainAnalyzer.global.Symbol]],
        originalError.asInstanceOf[Option[splainAnalyzer.AbsTypeError]]
      )
    case ImplicitErrorSpecifics.NotFound(param) =>
      splainAnalyzer.ImplicitErrorSpecifics.NotFound(param.asInstanceOf[splainAnalyzer.global.Symbol])
  }

  def convert: ImplicitError => splainAnalyzer.ImplicitError = { ee =>
    import ee._

    val result = splainAnalyzer.ImplicitError(
      tpe.asInstanceOf[splainAnalyzer.global.Type],
      candidate.asInstanceOf[splainAnalyzer.global.Tree],
      nesting,
      convertSpecifics(specifics)
    )

    result
  }

  object SplainAnalyzerPlugin extends global.analyzer.AnalyzerPlugin {

    override def noImplicitFoundError(param: Symbol, errors: List[ImplicitError], previous: String): String = {

      val convertedErrors = errors.map(convert)
      val result = splainAnalyzer.formatImplicitError(
        param.asInstanceOf[splainAnalyzer.global.Symbol],
        convertedErrors,
        ""
      )

      result
    }

    override def pluginsNotifyImplicitSearchResult(result: global.analyzer.SearchResult): Unit = {
      val r = super.pluginsNotifyImplicitSearchResult(result)
      r
    }

    override def pluginsNotifyImplicitSearch(search: global.analyzer.ImplicitSearch): Unit = {

//      error("dummy!")
      val tree = search.tree
      val pos = search.pos

      val tt = search.pt
      val context: global.analyzer.Context = search.context

      super.pluginsNotifyImplicitSearch(search)
    }
  }

  global.analyzer.addAnalyzerPlugin(SplainAnalyzerPlugin)
}
