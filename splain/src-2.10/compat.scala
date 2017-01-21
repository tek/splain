package splain

import tools.nsc._, typechecker._
import collection.mutable.ListBuffer
import annotation.tailrec
import reflect.internal.util.Statistics
import ImplicitsStats._

object OptionOps
{
  def contains[A](a: A)(o: Option[A]): Boolean = o.exists(_ == a)
}

trait Compat
{ self: Analyzer =>
  import global._

  trait TyperExtra
  { self: Typer =>
    def NoImplicitFoundError(tree: Tree, param: Symbol)
    (implicit context: Context): Unit = {
      if (featureImplicits) noImplicitError(tree, param)
      else TyperErrorGen.NoImplicitFoundError(tree, param)
    }

    override def applyImplicitArgs(fun: Tree): Tree = fun.tpe match {
      case MethodType(params, _) =>
        val argResultsBuff = new ListBuffer[SearchResult]()
        val argBuff = new ListBuffer[Tree]()
        var paramFailed = false

        def mkPositionalArg(argTree: Tree, paramName: Name) = argTree
        def mkNamedArg(argTree: Tree, paramName: Name) =
          atPos(argTree.pos)(new AssignOrNamedArg(Ident(paramName), (argTree)))
        var mkArg: (Tree, Name) => Tree = mkPositionalArg

        for(param <- params) {
          var paramTp = param.tpe
          for(ar <- argResultsBuff)
            paramTp = paramTp.subst(ar.subst.from, ar.subst.to)

          val res = if (paramFailed || (paramTp.isError && {paramFailed = true; true})) SearchFailure else inferImplicit(fun, paramTp, context.reportErrors, false, context)
          argResultsBuff += res

          if (res.isSuccess) {
            argBuff += mkArg(res.tree, param.name)
          } else {
            mkArg = mkNamedArg // don't pass the default argument (if any) here, but start emitting named arguments for the following args
            if (!param.hasDefault && !paramFailed) {
              context.errBuffer.find(_.kind == ErrorKinds.Divergent) match {
                case Some(divergentImplicit) if !settings.Xdivergence211.value =>
                  // DivergentImplicit error has higher priority than "no implicit found"
                  // no need to issue the problem again if we are still in silent mode
                  if (context.reportErrors) {
                    context.issue(divergentImplicit)
                    context.condBufferFlush(_.kind  == ErrorKinds.Divergent)
                  }
                case Some(divergentImplicit: DivergentImplicitTypeError) if settings.Xdivergence211.value =>
                  if (context.reportErrors) {
                    context.issue(divergentImplicit.withPt(paramTp))
                    context.condBufferFlush(_.kind  == ErrorKinds.Divergent)
                  }
                case None =>
                  implicit val ctx: Context = context
                  NoImplicitFoundError(fun, param)
              }
              paramFailed = true
            }
            /* else {
            TODO: alternative (to expose implicit search failure more) -->
            resolve argument, do type inference, keep emitting positional args, infer type params based on default value for arg
            for (ar <- argResultsBuff) ar.subst traverse defaultVal
            val targs = exprTypeArgs(context.undetparams, defaultVal.tpe, paramTp)
            substExpr(tree, tparams, targs, pt)
            }*/
          }
        }

        val args = argBuff.toList
        for (ar <- argResultsBuff) {
          ar.subst traverse fun
          for (arg <- args) ar.subst traverse arg
        }

        new ApplyToImplicitArgs(fun, args) setPos fun.pos
      case ErrorType =>
        fun
    }
  }

  object TermName
  {
    def apply(n: String) = newTermName(n)
  }

  override def newTyper(context: Context): Typer =
    new Typer(context) with TyperExtra
}

trait ImplicitsCompat
extends ImplicitChains
{ self: Analyzer with Compat =>
  import global._
  import typeDebug._

  def inferImplicitPre(shouldPrint: Boolean, tree: Tree, pt: Type,
    isView: Boolean, context: Context) = {
      typer.printInference("[infer %s] %s with pt=%s in %s".format(
        if (isView) "view" else "implicit",
        tree, pt, context.owner.enclClass)
      )
      typer.printTyping(
        ptBlock("infer implicit" + (if (isView) " view" else ""),
          "tree"        -> tree,
          "pt"          -> pt,
          "undetparams" -> context.outer.undetparams
          )
        )
      typer.indentTyping()
      if (printInfers && !tree.isEmpty && !context.undetparams.isEmpty)
        typer.printTyping("typing implicit: %s %s".format(tree,
          context.undetparamsString))
  }

  def inferImplicitPost(result: SearchResult, saveAmbiguousDivergent: Boolean,
    context: Context, implicitSearchContext: Context) = {
      if ((result.isFailure || !settings.Xdivergence211.value) &&
        saveAmbiguousDivergent && implicitSearchContext.hasErrors) {
        context
          .updateBuffer(implicitSearchContext.errBuffer
          .filter(err => err.kind == ErrorKinds.Ambiguous ||
            err.kind == ErrorKinds.Divergent))
        debugwarn("update buffer: " + implicitSearchContext.errBuffer)
      }
      typer.printInference("[infer implicit] inferred " + result)
      context.undetparams =
        context.undetparams filterNot result.subst.from.contains
      typer.deindentTyping()
      typer.printTyping("Implicit search yielded: "+ result)
  }

  class ImplicitSearchCompat(tree: Tree, pt: Type, isView: Boolean,
    context0: Context, pos0: Position = NoPosition)
  extends ImplicitSearchImpl(tree, pt, isView, context0, pos0)
  with TyperExtra
  {
    override val infer =
      if (featureBounds) new Inferencer(context0) with InferencerImpl {}
      else new Inferencer(context0) with InferencerCompat

    trait InferencerCompat
    { self: Inferencer =>
      override def isCoercible(tp: Type, pt: Type): Boolean = undoLog undo {
        tp.isError || pt.isError || getContext.implicitsEnabled &&
          inferView(EmptyTree, tp, pt, false) != EmptyTree
      }
    }
  }
}

class SplainPlugin(val global: Global)
extends Plugin
{ plugin =>
  def echo(msg: String) = ()
}
