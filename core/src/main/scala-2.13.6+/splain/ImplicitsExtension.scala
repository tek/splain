package splain

import scala.collection.mutable
import scala.tools.nsc.typechecker

trait ImplicitsExtension extends typechecker.Implicits {
  self: SplainAnalyzer =>

  import global._

  // TODO: add these into settings
  def settingVImplicitDiverging = true
  def settingVImplicitDivergingThreshold = 100

  case class DivergingImplicitErrorView(self: DivergentImplicitTypeError) {

    lazy val errMsg: String = {

      val formattedPT = showFormatted(formatType(self.pt0, top = false))

      s"diverging implicit expansion for type ${formattedPT}\nstarting with ${self.sym.fullLocationString}"
    }
  }

  object ImplicitHistory {

    @volatile protected var _currentGlobal: Global = _

    case class Global() {

      val byPosition = mutable.HashMap.empty[PositionIndex, Local]
    }

    case class Local() {

      object DivergingImplicitErrors {

        val errors = mutable.ArrayBuffer.empty[DivergentImplicitTypeError]

        def push(v: DivergentImplicitTypeError): Unit = {
          errors.addOne(v)
        }

        val linkedErrors = mutable.HashSet.empty[DivergentImplicitTypeError]

        def getUnlinkedMsgs: Seq[String] = {

          val Seq(msgs, linkedMsgs) = Seq(errors.toSeq, linkedErrors.toSeq).map { seq =>
            seq.map(v => DivergingImplicitErrorView(v).errMsg).distinct
          }

          val linkedMsgSet = linkedMsgs.toSet

          val result = msgs.filterNot { str =>
            linkedMsgSet.contains(str)
          }

          result
        }

        val logs = mutable.ArrayBuffer.empty[String]
        // unused messages & comments will be displayed at the end of the implicit error
      }
    }

    case class PositionIndex(
        pos: Position
    ) {}

    def currentGlobal: Global = Option(_currentGlobal).getOrElse {
      ImplicitHistory.synchronized {
        val result = Global()
        _currentGlobal = result
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

    import ImplicitHistory._

    def getResult = super.inferImplicit(tree, pt, reportAmbiguous, isView, context, saveAmbiguousDivergent, pos)

    if (settings.Vimplicits && settingVImplicitDiverging) {

      val posII = ImplicitHistory.PositionIndex(
        tree.pos
      )

      val local = currentGlobal.byPosition.getOrElseUpdate(posII, Local())
      val previousSimilarErrors = local.DivergingImplicitErrors.errors.filter { ee =>
        ee.underlyingTree equalsStructure tree
      }

      val previousSimilarErrorsN = previousSimilarErrors.size
      if (previousSimilarErrorsN >= settingVImplicitDivergingThreshold) {

        local.DivergingImplicitErrors.logs +=
          s"""
             |Implicit search for ${tree}
             |has reported ${previousSimilarErrorsN} diverging errors.
             |Terminated!
             |    at ${pos.showDebug}
             |""".trim.stripMargin

        s"Terminating implicit search for $tree at ${pos.showDebug} " +
          s"after reporting $settingVImplicitDivergingThreshold Diverging implicit errors"
        return SearchFailure
      }

      val result = getResult

      val divergingErrors = context.reporter.errors.collect {
        case ee: DivergentImplicitTypeError =>
          ee
      }

      divergingErrors.foreach { ee =>
        local.DivergingImplicitErrors.push(ee)

//        require(ee.pt0 == pt, s"mismatch! ${ee.pt0} != $pt")

        context.reporter.retainDivergentErrorsExcept(ee)
      }

//      val cc = result.isSuccess || result == SearchFailure

      result
    } else {

      getResult
    }
  }
}
