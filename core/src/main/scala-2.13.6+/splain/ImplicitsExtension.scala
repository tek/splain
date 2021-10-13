package splain

import scala.collection.mutable
import scala.tools.nsc.Reporting.WarningCategory
import scala.tools.nsc.typechecker

trait ImplicitsExtension extends typechecker.Implicits {
  self: SplainAnalyzer =>

  import global._

  // TODO: remove duplicate code after merging into scalac
  def DIEError(tree: Tree, param: Symbol, dte: DivergentImplicitTypeError)(
      implicit
      context: Context
  ): Unit = {
    val (isSupplement, annotationMsg) = NoImplicitFoundAnnotation(tree, param)
    def defaultErrMsg = {
      val paramName = param.name
      val paramTp = param.tpe
      def evOrParam =
        if (paramName startsWith nme.EVIDENCE_PARAM_PREFIX)
          "evidence parameter of type"
        else
          s"parameter $paramName:"

      s"$dte$annotationMsg"
    }
    val errMsg = splainPushOrReportNotFound(tree, param, annotationMsg)
//    error("DUMMY!")
    ErrorUtils.issueNormalTypeError(tree, if (errMsg.isEmpty) defaultErrMsg else errMsg)
  }

  /**
    * DIE means diverging implicit expansion
    */
  trait DIEReporterExtension extends ContextReporter {

    override def reportFirstDivergentError(fun: Tree, param: Symbol, paramTp: Type)(
        implicit
        context: Context
    ): Unit = {

      super.reportFirstDivergentError(fun, param, paramTp)

//      errors.collectFirst {
//        case dte: DivergentImplicitTypeError => dte
//      } match {
//        case Some(divergent) =>
//          DIEError(fun, param, divergent)(context)
//
//        case _ =>
//          NoImplicitFoundError(fun, param)(context)
//      }
    }
  }

  object DIEReporterExtension {

    class ImmediateReporter(
        _errorBuffer: mutable.LinkedHashSet[AbsTypeError] = null,
        _warningBuffer: mutable.LinkedHashSet[(Position, String, WarningCategory, Symbol)] = null
    ) extends ContextReporter(_errorBuffer, _warningBuffer)
        with DIEReporterExtension {

      override def makeBuffering: ContextReporter =
        new BufferingReporter(errorBuffer, warningBuffer)

      def error(pos: Position, msg: String): Unit = reporter.error(pos, msg)
    }

    class BufferingReporter(
        _errorBuffer: mutable.LinkedHashSet[AbsTypeError] = null,
        _warningBuffer: mutable.LinkedHashSet[(Position, String, WarningCategory, Symbol)] = null
    ) extends ContextReporter(_errorBuffer, _warningBuffer)
        with DIEReporterExtension {
      override def isBuffering = true

      override def issue(err: AbsTypeError)(
          implicit
          context: Context
      ): Unit = errorBuffer += err

      // this used to throw new TypeError(pos, msg) -- buffering lets us report more errors (test/files/neg/macro-basic-mamdmi)
      // the old throwing behavior was relied on by diagnostics in manifestOfType
      def error(pos: Position, msg: String): Unit = errorBuffer += TypeErrorWrapper(new TypeError(pos, msg))

      override def warning(pos: Position, msg: String, category: WarningCategory, site: Symbol): Unit =
        warningBuffer += ((pos, msg, category, site))

      override protected def handleSuppressedAmbiguous(err: AbsAmbiguousTypeError): Unit = errorBuffer += err

      // TODO: emit all buffered errors, warnings
      override def makeImmediate: ContextReporter =
        new ImmediateReporter(errorBuffer, warningBuffer)
    }

    def swapOutReporter(context: Context): Context = {
      val oldReporter = context.reporter

      val _reporter = oldReporter match {
        case _: DIEReporterExtension =>
          oldReporter
        case _ =>
          if (oldReporter.isBuffering) {
//            new DIEReporterExtension.BufferingReporter(
//              oldReporter.errors.to(mutable.LinkedHashSet),
//              oldReporter.warnings.to(mutable.LinkedHashSet)
//            )
            oldReporter
          } else {
            new DIEReporterExtension.ImmediateReporter(
              oldReporter.errors.to(mutable.LinkedHashSet),
              oldReporter.warnings.to(mutable.LinkedHashSet)
            )
          }
      }

      val newContext = context.makeNewScope(
        context.tree,
        context.owner,
        reporter = _reporter
      )
      newContext
    }
  }

  override def inferImplicit(
      tree: Tree,
      pt: Type,
      reportAmbiguous: Boolean,
      isView: Boolean,
      context: Context,
      saveAmbiguousDivergent: Boolean,
      pos: Position
  ): SearchResult = {

    val result = super.inferImplicit(tree, pt, reportAmbiguous, isView, context, saveAmbiguousDivergent, pos)

    val dieErrors = context.reporter.errors.collect {
      case ee: DivergentImplicitTypeError => ee
    }

    dieErrors.foreach { ee =>
      context.reporter.retainDivergentErrorsExcept(ee)
    }

    result
  }
}
