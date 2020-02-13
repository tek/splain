package splain

import scala.collection.mutable
import scala.tools.nsc._

class SplainPlugin(val global: Global) extends SplainPluginCompat
{

  val analyzerField = classOf[Global].getDeclaredField("analyzer")
  analyzerField.setAccessible(true)
  analyzerField.set(global, analyzer)

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
    val oldScs @ List(oldNamer@_, oldPackageobjects@_, oldTyper@_) =
      List(subcomponentNamed("namer"),
        subcomponentNamed("packageobjects"),
        subcomponentNamed("typer"))
    val newScs = List(analyzer.namerFactory,
      analyzer.packageObjects,
      analyzer.typerFactory)
    phasesSet --= oldScs
    phasesSet ++= newScs
  }

  override def init(options: List[String], error: String => Unit): Boolean = {
    def invalid(opt: String) = error(s"splain: invalid option `$opt`")
    def setopt(key: String, value: String) = {
      if (opts.contains(key)) opts.update(key, value)
      else invalid(key)
    }
    options.foreach { opt =>
      opt.split(":").toList match {
        case key :: value :: Nil => setopt(key, value)
        case key :: Nil => setopt(key, "true")
        case _ => invalid(opt)
      }
    }
    enabled
  }
}

abstract class SplainPluginLike extends plugins.Plugin {

  val name = "splain"
  val description = "better types and implicit errors"
  val components = Nil

  val keyAll = "all"
  val keyImplicits = "implicits"
  val keyFoundReq = "foundreq"
  val keyInfix = "infix"
  val keyBounds = "bounds"
  val keyColor = "color"
  val keyBreakInfix = "breakinfix"
  val keyCompact = "compact"
  val keyTree = "tree"
  val keyBoundsImplicits = "boundsimplicits"
  val keyTruncRefined = "truncrefined"
  val keyRewrite = "rewrite"
  val keyKeepModules = "keepmodules"

  val opts: mutable.Map[String, String] = mutable.Map(
    keyAll -> "true",
    keyImplicits -> "true",
    keyFoundReq -> "true",
    keyInfix -> "true",
    keyBounds -> "false",
    keyColor -> "true",
    keyBreakInfix -> "0",
    keyCompact -> "false",
    keyTree -> "true",
    keyBoundsImplicits -> "true",
    keyTruncRefined -> "0",
    keyRewrite -> "",
    keyKeepModules -> "0"
  )

  def opt(key: String, default: String) = opts.getOrElse(key, default)

  def enabled = opt("all", "true") == "true"

  def boolean(key: String) = enabled && opt(key, "true") == "true"

  def int(key: String): Option[Int] =
    if (enabled) opts.get(key).flatMap(a => scala.util.Try(a.toInt).toOption)
    else None
}
