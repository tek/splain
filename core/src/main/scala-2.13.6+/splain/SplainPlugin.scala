package splain

import scala.tools.nsc._

class SplainPlugin(val global: Global) extends SplainPluginLike {

  import global._
  import analyzer._

  lazy val splainAnalyzer: SplainAnalyzer = new SplainAnalyzer(global)

  override def init(options: List[String], error: String => Unit): Boolean = {
    def invalid(opt: String) = error(s"splain: invalid option `$opt`")
    def setopt(key: String, value: String) =
      if (opts.contains(key))
        opts.update(key, value)
      else
        invalid(key)
    options.foreach { opt =>
      opt.split(":").toList match {
        case key :: value :: Nil =>
          setopt(key, value)
        case key :: Nil =>
          setopt(key, "true")
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
