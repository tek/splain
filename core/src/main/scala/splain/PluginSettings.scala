package splain

import scala.collection.mutable

case class PluginSettings(pluginOpts: mutable.Map[String, String]) {

  import PluginSettings._

  def opt(key: String, default: String): String = pluginOpts.getOrElse(key, default)

  def enabled: Boolean = opt(Key.enabled, "true") == "true"
  def enableAll: Boolean = opt(Key.enableAll, "false") == "true"

  def boolean(key: String): Boolean = {
    if (!enabled) false
    else if (enableAll) true
    else opt(key, "true") == "true"
  }

  def intOptional(key: String): Option[Int] =
    if (enabled)
      pluginOpts.get(key).flatMap(a => scala.util.Try(a.toInt).toOption)
    else
      None

  def int(key: String): Int = {
    val opt = intOptional(key)

    opt.getOrElse(throw new SplainInternalError(s"$key is not defined"))
  }

  // once read, changing pluginOpts will no longer be useful
  def implicitDiverging: Boolean = boolean(PluginSettings.Key.implicitDiverging)

  def implicitDivergingMaxDepth: Int = int(Key.implicitDivergingMaxDepth)

  def typeDetail: Int = int(PluginSettings.Key.typeDetail)

  object TypeDetail {}

  def showTypeReduction: Boolean = boolean(PluginSettings.Key.typeReduction)

  def showTypeDefPosition: Boolean = boolean(PluginSettings.Key.typeDefPosition)

  def typeDiffsDetail: Int = int(PluginSettings.Key.typeDiffsDetail)

  object TypeDiffsDetail {

    final def disambiguation: Boolean = typeDiffsDetail >= 2
    final def builtInMsg: Boolean = typeDiffsDetail >= 3
    final def builtInMsgAlways: Boolean = typeDiffsDetail >= 4
  }

  def debug: Boolean = boolean(PluginSettings.Key.debug)
}

object PluginSettings {

  // TODO: this object can be inlined if defaults are defined within PluginSettings
  object Key {

    val enabled = "enabled"
    val enableAll = "enableAll"

    val implicitDiverging = "Vimplicits-diverging"

    val implicitDivergingMaxDepth = "Vimplicits-diverging-max-depth"

    val typeReduction = "Vtype-reduction"

    val typeDefPosition = "Vtype-def-position"

    val typeDetail = "Vtype-detail"

    val typeDiffsDetail = "Vtype-diffs-detail"

    val debug = "Vdebug"
  }

  val inits: Map[String, String] = Map(
    Key.enabled -> "true",
    Key.enableAll -> "false",
    Key.implicitDiverging -> "false",
    Key.implicitDivergingMaxDepth -> "100",
    Key.typeReduction -> "false",
    Key.typeDefPosition -> "false",
    Key.typeDetail -> "1",
    Key.typeDiffsDetail -> "1",
    Key.debug -> "false"
  )
}
