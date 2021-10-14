package splain

import scala.collection.mutable

case class PluginSettings(pluginOpts: mutable.Map[String, String]) {

  import PluginSettings._

  def opt(key: String, default: String): String = pluginOpts.getOrElse(key, default)

  def enabled: Boolean = opt(Key.all, "true") == "true"

  def boolean(key: String): Boolean = enabled && opt(key, "true") == "true"

  def int(key: String): Option[Int] =
    if (enabled)
      pluginOpts.get(key).flatMap(a => scala.util.Try(a.toInt).toOption)
    else
      None

  // once read, changing pluginOpts will no longer be useful
  lazy val implicitDiverging: Boolean = boolean(PluginSettings.Key.implicitDiverging)

  lazy val implicitDivergingMaxDepth: Int = int(Key.implicitDivergingMaxDepth)
    .getOrElse(
      throw new UnsupportedOperationException(s"${Key.implicitDivergingMaxDepth} is not defined")
    )

}

object PluginSettings {

  object Key {

    //  val keyImplicits = "implicits"
    //  val keyFoundReq = "foundreq"
    //  val keyInfix = "infix"
    //  val keyBounds = "bounds"
    //  val keyColor = "color"
    //  val keyBreakInfix = "breakinfix"
    //  val keyCompact = "compact"
    //  val keyTree = "tree"
    //  val keyBoundsImplicits = "boundsimplicits"
    //  val keyTruncRefined = "truncrefined"
    //  val keyRewrite = "rewrite"
    //  val keyKeepModules = "keepmodules"

    val all = "all"
    val implicitDiverging = "Vimplicits-diverging"

    val implicitDivergingMaxDepth = "Vimplicits-diverging-max-depth"
  }

  val defaults = Map(
    Key.all -> "true",
    Key.implicitDiverging -> "false",
    Key.implicitDivergingMaxDepth -> "100"
    //    keyImplicits -> "true",
    //    keyFoundReq -> "true",
    //    keyInfix -> "true",
    //    keyBounds -> "false",
    //    keyColor -> "true",
    //    keyBreakInfix -> "0",
    //    keyCompact -> "false",
    //    keyTree -> "true"
    //    keyBoundsImplicits -> "true",
    //    keyTruncRefined -> "0",
    //    keyRewrite -> "",
    //    keyKeepModules -> "0"
  )
}
