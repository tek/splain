package splain

import util.Try
import collection.mutable
import tools.nsc._
import typechecker.Analyzer

import StringColor._

class Options
{
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

  def process(options: List[String], error: String => Unit) = {
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
    keyTruncRefined -> "0"
  )

  def opt(key: String, default: String) = opts.getOrElse(key, default)

  def enabled = opt("all", "true") == "true"

  def boolean(key: String) = enabled && opt(key, "true") == "true"

  def int(key: String) =
    if (enabled) opts.get(key).flatMap(a => Try(a.toInt).toOption)
    else None
}

trait Plugin
extends plugins.Plugin
{
  val name = "splain"
  val description = "better types and implicit errors"
  val components = Nil

  override def processOptions(opt: List[String], error: String => Unit) = opts.process(opt, error)

  lazy val opts = new Options
}

class Formatter(val analyzer: Analyzer, opts: Options)
extends Formatting
{
  import analyzer._

  import opts._

  def featureImplicits = boolean(keyImplicits)
  def featureFoundReq = boolean(keyFoundReq)
  def featureInfix = boolean(keyInfix)
  def featureBounds = boolean(keyBounds)
  def featureColor = boolean(keyColor)
  def featureBreakInfix = int(keyBreakInfix).filterNot(_ == 0)
  def featureCompact = boolean(keyCompact)
  def featureTree = boolean(keyTree)
  def featureBoundsImplicits = boolean(keyBoundsImplicits)
  def featureTruncRefined = int(keyTruncRefined).filterNot(_ == 0)

  def formatNestedImplicit(err: ImpFailReason): (String, List[String], Int) = {
    val candidate = err.cleanCandidate
    val problem = s"${candidate.red} invalid because"
    val reason = err match {
      case e: ImpError => implicitMessage(e.param)
      case e: NonConfBounds => formatNonConfBounds(e)
    }
    (problem, reason, err.nesting)
  }

  def hideImpError(error: ImpFailReason) =
    (error.candidateName.toString == "mkLazy") || (!featureBoundsImplicits && (
      error match {
        case NonConfBounds(_, _, _, _, _) => true
        case _ => false
      }
      ))
}

class SplainPlugin(val global: Global)
extends Plugin
{ plug =>
  import global._
  import analyzer._

  lazy val formatter = new Formatter(analyzer, opts)

  object analyzerPlugin
  extends analyzer.AnalyzerPlugin
  {
    override def noImplicitFoundError(param: Symbol, errors: List[ImpFailReason]): Option[String] = {
      val p = param.asInstanceOf[formatter.analyzer.global.Symbol]
      val e = errors.asInstanceOf[List[formatter.analyzer.ImpFailReason]]
      Some(formatter.formatImplicitError(p, e))
    }

    override def foundReqMsg(found: Type, req: Type): Option[String] = {
      val f = found.asInstanceOf[formatter.analyzer.global.Type]
      val r = req.asInstanceOf[formatter.analyzer.global.Type]
      Some(";\n" + formatter.showFormattedL(formatter.formatDiff(f, r, true), true).indent.joinLines)
    }
  }

  global.analyzer.addAnalyzerPlugin(analyzerPlugin)
}
