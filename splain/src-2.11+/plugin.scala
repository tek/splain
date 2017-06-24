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

trait Formatting
extends core.Formatting
{
  import analyzer._
  import global._

  // TODO split non conf bounds
  def formatNonConfBounds(err: NonConfBounds): List[String] = {
    val params = bracket(err.tparams.map(_.defString))
    val tpes = bracket(err.targs map showType)
    List("nonconformant bounds;", tpes.red, params.green)
  }

  def formatNestedImplicit(err: ImpFailReason): (String, List[String], Int)

  def hideImpError(error: ImpFailReason): Boolean

  def indentTree(tree: List[(String, List[String], Int)], baseIndent: Int): List[String] = {
    val nestings = tree.map(_._3).distinct.sorted
    tree
      .flatMap {
        case (head, tail, nesting) =>
          val ind = baseIndent + nestings.indexOf(nesting).abs
          indentLine(head, ind, "――") :: indent(tail, ind)
      }
  }

  def formatIndentTree(chain: List[ImpFailReason], baseIndent: Int) = {
    val formatted = chain map formatNestedImplicit
    indentTree(formatted, baseIndent)
  }

  def deepestLevel(chain: List[ImpFailReason]) = {
    chain.foldLeft(0)((z, a) => if (a.nesting > z) a.nesting else z)
  }

  def formatImplicitChainTreeCompact(chain: List[ImpFailReason]): Option[List[String]] = {
    chain
      .headOption
      .map { head =>
        val max = deepestLevel(chain)
        val leaves = chain.drop(1).dropWhile(_.nesting < max)
        val base = if (head.nesting == 0) 0 else 1
        val (fhh, fht, fhn) = formatNestedImplicit(head)
        val spacer = if (leaves.nonEmpty && leaves.length < chain.length) List("⋮".blue) else Nil
        val fh = (fhh, fht ++ spacer, fhn)
        val ft = leaves map formatNestedImplicit
        indentTree(fh :: ft, base)
      }
  }

  def formatImplicitChainTreeFull(chain: List[ImpFailReason]): List[String] = {
    val baseIndent = chain.headOption.map(_.nesting).getOrElse(0)
    formatIndentTree(chain, baseIndent)
  }

  def formatImplicitChainFlat(chain: List[ImpFailReason]): List[String] = {
    chain map formatNestedImplicit flatMap { case (h, t, _) => h :: t }
  }

  def formatImplicitChainTree(chain: List[ImpFailReason]): List[String] = {
    val compact = if (featureCompact) formatImplicitChainTreeCompact(chain) else None
    compact getOrElse formatImplicitChainTreeFull(chain)
  }

  def formatImplicitChain(chain: List[ImpFailReason]): List[String] = {
    if (featureTree) formatImplicitChainTree(chain)
    else formatImplicitChainFlat(chain)
  }

  /**
   * Remove duplicates and special cases that should not be shown.
   * In some cases, candidates are reported twice, once as `Foo.f` and once as
   * `f`. [[ImpFailReason.equals]] checks the simple names for identity, which
   * is suboptimal, but works for 99% of cases.
   * Special cases are handled in [[hideImpError]]
   */
  def formatNestedImplicits(errors: List[ImpFailReason]) = {
    val visible = errors filterNot hideImpError
    val chains = splitChains(visible).map(_.distinct).distinct
    chains map formatImplicitChain flatMap ("" :: _) drop 1
  }

  def splitChains(errors: List[ImpFailReason]): List[List[ImpFailReason]] = {
    errors.foldRight(Nil: List[List[ImpFailReason]]) {
      case (a, chains @ ((chain @ (prev :: _)) :: tail)) =>
        if (a.nesting > prev.nesting) List(a) :: chains
        else (a :: chain) :: tail
      case (a, _) =>
        List(List(a))
    }
  }

  def formatImplicitError(param: Symbol, errors: List[ImpFailReason]) = {
    val stack = formatNestedImplicits(errors)
    val nl = if (errors.nonEmpty) "\n" else ""
    val ex = stack.mkString("\n")
    val pre = "implicit error;\n"
    val msg = implicitMessage(param).mkString("\n")
    s"$pre$msg$nl$ex"
  }
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
