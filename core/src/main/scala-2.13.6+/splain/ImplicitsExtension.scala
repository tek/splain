package splain

import scala.collection.mutable
import scala.tools.nsc.typechecker

trait ImplicitsExtension extends typechecker.Implicits {
  self: SplainAnalyzer =>

  import global._

  object ReportedErrors {

    case class ErrorIndex(
        pos: Position
//        missingTpeSym: Symbol,
//        fnSym: Symbol
    ) {}

    val cache = mutable.HashMap.empty[ErrorIndex, mutable.ArrayBuffer[AbsTypeError]]
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

    val divergings = context.reporter.errors.collect {
      case ee: DivergentImplicitTypeError =>
        ee
    }

    divergings.foreach { ee =>
//      require(tree == ee.underlyingTree) // TODO: remove this

      val ii = ReportedErrors.ErrorIndex(
        tree.pos
//        ee.sym
      )

      val vs = ReportedErrors.cache.getOrElseUpdate(ii, mutable.ArrayBuffer.empty)
      vs.addOne(ee)

      context.reporter.retainDivergentErrorsExcept(ee)
    }

    result
  }
}
