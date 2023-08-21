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

  lazy val typeNormalization: Boolean = boolean(PluginSettings.Key.typeNormalization)

  lazy val debug: Boolean = boolean(Key.debug)
}

object PluginSettings {

  object Key {

    val all = "all"
    val implicitDiverging = "Vimplicits-diverging"

    val implicitDivergingMaxDepth = "Vimplicits-diverging-max-depth"

    val typeNormalization = "Vtype-normalization"

    val debug = "debug"
  }

  val defaults: Map[String, String] = Map(
    Key.all -> "true",
    Key.implicitDiverging -> "false",
    Key.implicitDivergingMaxDepth -> "100",
    Key.typeNormalization -> "false",
    Key.debug -> "false"
  )
}
