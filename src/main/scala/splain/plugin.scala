package splain

import util.Try
import tools.nsc._
import collection.mutable

trait TypeDiagnostics
extends typechecker.TypeDiagnostics
with Formatting
{ self: Analyzer =>
  import global._

  def featureFoundReq: Boolean

  def echo(msg: String): Unit

  def showStats[A](desc: String, run: => A): A = {
    val ret = run
    if (sys.env.contains("SPLAIN_CACHE_STATS"))
      echo(s"$desc entries/hits: $cacheStats")
    ret
  }

  def foundReqMsgShort(found: Type, req: Type): TypeRepr =
    showStats("foundreq", showFormattedL(formatDiff(found, req, true), true))

  override def foundReqMsg(found: Type, req: Type): String =
    if (featureFoundReq) ";\n" + foundReqMsgShort(found, req).indent.joinLines
    else super.foundReqMsg(found, req)
}

trait Analyzer
extends typechecker.Analyzer
with ImplicitsCompat
with TypeDiagnostics

trait Plugin
extends plugins.Plugin
{
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

  override def processOptions(options: List[String], error: String => Unit) = {
    def invalid(opt: String) = error(s"splain: invalid option `$opt`")
    def setopt(key: String, value: String) = {
      if (opts.contains(key)) opts.update(key, value)
      else invalid(key)
    }
    options foreach { opt =>
      opt.split(":").toList match {
        case key :: value :: Nil => setopt(key, value)
        case key :: Nil => setopt(key, "true")
        case _ => invalid(opt)
      }
    }
  }

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

  def int(key: String) =
    if (enabled) opts.get(key).flatMap(a => Try(a.toInt).toOption)
    else None
}
