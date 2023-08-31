package splain

import scala.collection.mutable

case class PluginSettings(pluginOpts: mutable.Map[String, String]) {

  import PluginSettings._

  def opt(key: String, default: String): String = pluginOpts.getOrElse(key, default)

  def enabled: Boolean = opt(Key.enabled, "true") == "true"

  def boolean(key: String): Boolean = enabled && opt(key, "true") == "true"

  def int(key: String): Option[Int] =
    if (enabled)
      pluginOpts.get(key).flatMap(a => scala.util.Try(a.toInt).toOption)
    else
      None

  // once read, changing pluginOpts will no longer be useful
  def implicitDiverging: Boolean = boolean(PluginSettings.Key.implicitDiverging)

  def implicitDivergingMaxDepth: Int = int(Key.implicitDivergingMaxDepth)
    .getOrElse(
      throw new UnsupportedOperationException(s"${Key.implicitDivergingMaxDepth} is not defined")
    )

  def typeReduction: Boolean = boolean(PluginSettings.Key.typeReduction)

  def debug: Boolean = boolean(Key.debug)
}

object PluginSettings {

  // TODO: this object can be inlined if defaults are defined within PluginSettings
  object Key {

    val enabled = "enabled"
    val implicitDiverging = "Vimplicits-diverging"

    val implicitDivergingMaxDepth = "Vimplicits-diverging-max-depth"

    val typeReduction = "Vtype-reduction"

    val debug = "debug"
  }

  val defaults: Map[String, String] = Map(
    Key.enabled -> "true",
    Key.implicitDiverging -> "false",
    Key.implicitDivergingMaxDepth -> "100",
    Key.typeReduction -> "false",
    Key.debug -> "false"
  )
}
