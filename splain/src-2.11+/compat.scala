package splain

import tools.nsc._
import collection.mutable

object OptionOps
{
  def contains[A](a: A)(o: Option[A]): Boolean = o.contains(a)
}

trait Compat
{ self: Analyzer =>
  import global._

  object TermNameCompat
  {
    def apply(n: String) = TermName(n)
  }
}

trait ImplicitsCompat
extends ImplicitChains
{ self: Analyzer =>
  import global._

  def inferImplicitPre(shouldPrint: Boolean, tree: Tree, pt: Type,
    isView: Boolean, context: Context) = {
    if (shouldPrint)
      typingStack.printTyping(tree, "typing implicit: %s %s"
        .format(tree, context.undetparamsString))
  }

  def inferImplicitPost(result: SearchResult, saveAmbiguousDivergent: Boolean,
    context: Context, implicitSearchContext: Context) = {
    if (result.isFailure && saveAmbiguousDivergent &&
      implicitSearchContext.reporter.hasErrors)
      implicitSearchContext.reporter
        .propagateImplicitTypeErrorsTo(context.reporter)
    context.undetparams =
      ((context.undetparams ++ result.undetparams)
        .filterNot(result.subst.from.contains)).distinct
  }

  class ImplicitSearchCompat(tree: Tree, pt: Type, isView: Boolean,
    context0: Context, pos0: Position = NoPosition)
  extends ImplicitSearchImpl(tree, pt, isView, context0, pos0)
  {
    override val infer =
      if (featureBounds) new InferencerCompat with InferencerImpl
      else new InferencerCompat

    class InferencerCompat
    extends Inferencer
    {
      def context = ImplicitSearchCompat.this.context

      override def isCoercible(tp: Type, pt: Type) =
        undoLog undo viewExists(tp, pt)
    }
  }

  override def NoImplicitFoundError(tree: Tree, param: Symbol)
  (implicit context: Context): Unit = {
    if (featureImplicits) noImplicitError(tree, param)
    else super.NoImplicitFoundError(tree, param)
  }
}

class SplainPlugin(val global: Global)
extends plugins.Plugin
{ plugin =>
  val analyzer =
    new { val global = plugin.global } with Analyzer {
      def featureImplicits = boolean(keyImplicits)
      def featureFoundReq = boolean(keyFoundReq)
      def featureInfix = boolean(keyInfix)
      def featureBounds = boolean(keyBounds)
      def featureColor = boolean(keyColor)
    }

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
    val oldScs @ List(oldNamer, oldPackageobjects, oldTyper) =
      List(subcomponentNamed("namer"),
        subcomponentNamed("packageobjects"),
        subcomponentNamed("typer"))
    val newScs = List(analyzer.namerFactory,
      analyzer.packageObjects,
      analyzer.typerFactory)
    phasesSet --= oldScs
    phasesSet ++= newScs
  }

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

  val name = "splain"
  val description = "better types and implicit errors"
  val components = Nil

  val keyAll = "all"
  val keyImplicits = "implicits"
  val keyFoundReq = "foundreq"
  val keyInfix = "infix"
  val keyBounds = "bounds"
  val keyColor = "color"

  val opts: mutable.Map[String, String] = mutable.Map(
    keyAll -> "true",
    keyImplicits -> "true",
    keyFoundReq -> "true",
    keyInfix -> "true",
    keyBounds -> "false",
    keyColor -> "true"
  )

  def opt(key: String, default: String) = opts.getOrElse(key, default)

  def enabled = opt("all", "true") == "true"

  def boolean(key: String) = enabled && opt(key, "true") == "true"
}
