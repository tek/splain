package splain

import scala.collection.mutable
import scala.tools.nsc._
import scala.tools.nsc.plugins.PluginComponent

trait SplainPluginLike extends plugins.Plugin {

  val name = "splain"
  val description = "better types and implicit errors"
  val components: List[PluginComponent] = Nil

  val opts: mutable.Map[String, String] = PluginSettings.inits.to(mutable.Map)
}
