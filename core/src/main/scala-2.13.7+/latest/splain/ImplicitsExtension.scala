package splain

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.tools.nsc.typechecker

trait ImplicitsExtension extends TyperCompatViews with typechecker.Implicits {
  self: SplainAnalyzer =>

  import global._

  object ImplicitsHistory {

    lazy val currentGlobal: Global = {
      val result = Global()
      result
    }

    case class Global() {

      val localByPosition: TrieMap[PositionIndex, Local] = TrieMap.empty
    }

    case class Local() {

      object DivergingImplicitErrors {

        val errors: mutable.ArrayBuffer[DivergentImplicitTypeError] = mutable.ArrayBuffer.empty

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

        val logs: mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty[String]
        // unused messages & comments will be displayed at the end of the implicit error
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

    import ImplicitsHistory._

    def getResult = super.inferImplicit(tree, pt, reportAmbiguous, isView, context, saveAmbiguousDivergent, pos)

    if (settings.Vimplicits.value && pluginSettings.implicitDiverging) {

      val posII = PositionIndex(
        tree.pos
      )

      val local = currentGlobal.localByPosition.getOrElseUpdate(posII, Local())
      val previousSimilarErrors = local.DivergingImplicitErrors.errors.filter { ee =>
        ee.underlyingTree equalsStructure tree
      }

      val previousSimilarErrorsN = previousSimilarErrors.size
      if (previousSimilarErrorsN >= pluginSettings.implicitDivergingMaxDepth) {

        local.DivergingImplicitErrors.logs +=
          s"""
             |Implicit search for $tree
             |has reported $previousSimilarErrorsN diverging errors
             |Terminated
             |    at ${pos.showDebug}
             |""".stripMargin.trim

//        s"Terminating implicit search for $tree at ${pos.showDebug} " +
//          s"after reporting ${settingVImplicitDivergingThreshold} Diverging implicit errors"
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

      val result: SearchResult = getResult
      result
    }
  }
}
