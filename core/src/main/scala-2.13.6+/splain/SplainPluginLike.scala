package splain

import scala.collection.mutable
import scala.tools.nsc._

trait SplainPluginLike extends plugins.Plugin {

  val name = "splain"
  val description = "better types and implicit errors"
  val components = Nil

  val keyAll = "all"
  val keyImplicits = "implicits"
  val keyFoundReq = "foundreq"
  val keyInfix = "infix"
  val keyBounds = "bounds"
  val keyColor = "color"
//  val keyBreakInfix = "breakinfix"
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
//    keyBreakInfix -> "0",
    keyCompact -> "false",
    keyTree -> "true",
    keyBoundsImplicits -> "true",
    keyTruncRefined -> "0",
    keyRewrite -> "",
    keyKeepModules -> "0"
  )

  def opt(key: String, default: String) = opts.getOrElse(key, default)

  def enabled: Boolean = opt("all", "true") == "true"

  def boolean(key: String) = enabled && opt(key, "true") == "true"

  def int(key: String): Option[Int] =
    if (enabled)
      opts.get(key).flatMap(a => scala.util.Try(a.toInt).toOption)
    else
      None
}
