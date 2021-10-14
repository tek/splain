package splain

import scala.collection.mutable
import scala.tools.nsc.typechecker

trait ImplicitsExtension extends typechecker.Implicits {
  self: SplainAnalyzer =>

  import global._

  case class ImplicitSession() {
    import ImplicitSession._

    object Diverging {

      val byPosition = mutable.HashMap.empty[PositionIndex, mutable.ArrayBuffer[DivergentImplicitTypeError]]
    }

  }

  object ImplicitSession {

    case class PositionIndex(
        pos: Position
    ) {}

    @volatile protected var _current: ImplicitSession = _

    def current: ImplicitSession = Option(_current).getOrElse {
      ImplicitSession.synchronized {
        val result = new ImplicitSession()
        _current = result
        result
      }
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

    if (settings.Vimplicits) {
      val divergingErrors = context.reporter.errors.collect {
        case ee: DivergentImplicitTypeError =>
          ee
      }

      divergingErrors.foreach { ee =>
        val ii = ImplicitSession.PositionIndex(
          tree.pos
        )

        val vs = ImplicitSession.current.Diverging.byPosition.getOrElseUpdate(ii, mutable.ArrayBuffer.empty)
        vs.addOne(ee)

        context.reporter.retainDivergentErrorsExcept(ee)
      }
    }

    result
  }
}
