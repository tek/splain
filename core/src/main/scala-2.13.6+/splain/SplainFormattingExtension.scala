package splain

import scala.tools.nsc.typechecker

trait SplainFormattingExtension extends typechecker.splain.SplainFormatting with SplainFormattersExtension {
  self: SplainAnalyzer =>

  import global._

  case class ImplicitErrorLink(
      fromTree: ImplicitError,
      fromHistory: DivergentImplicitTypeError
  ) {

    val sameCandidateTree = fromTree.candidate equalsStructure fromHistory.underlyingTree

    val samePendingType = fromTree.specifics match {
      case ss: ImplicitErrorSpecifics.NotFound =>
        fromHistory.pt0 =:= ss.param.tpe
      case _ =>
        false
    }

    val moreSpecificPendingType = fromTree.specifics match {
      case ss: ImplicitErrorSpecifics.NotFound =>
        fromHistory.pt0 <:< ss.param.tpe
      case _ =>
        false
    }

    val sameStartingWith = {
      fromHistory.sym.fullLocationString == fromTree.candidate.symbol.fullLocationString
    }

    lazy val divergingSearchStartingWithHere = sameStartingWith

    lazy val divergingSearchDiscoveredHere = sameCandidateTree && moreSpecificPendingType
  }

  object ImplicitErrorLink {}

  case class ImplicitErrorTree(
      error: ImplicitError,
      children: Seq[ImplicitErrorTree] = Nil
  ) {

    lazy val asChain: List[ImplicitError] = List(error) ++ children.flatMap(_.asChain)

    override def toString: String = {
      val formattedChain = formatImplicitChain(asChain)

      formattedChain.mkString("\n")
    }

  }

  override def formatNestedImplicit(err: ImplicitError): (String, List[String], Int) = {

    val base = super.formatNestedImplicit(err)

    object ErrorsInLocalHistory {

      lazy val posI = ImplicitHistory.PositionIndex(
        err.candidate.pos
      )

      lazy val localHistoryOpt: Option[ImplicitHistory.Local] =
        ImplicitHistory.currentGlobal.byPosition.get(posI)

      lazy val diverging: Seq[DivergentImplicitTypeError] = {

        localHistoryOpt.toSeq.flatMap { history =>
          history.DivergingImplicitErrors.errors
        }
      }
    }

    val reasons = {
      val baseReasons = base._2

      val discoveredHere = ErrorsInLocalHistory.diverging.find { inHistory =>
        val link = ImplicitErrorLink(err, inHistory)

        link.divergingSearchDiscoveredHere
      }

      discoveredHere match {
        case Some(ee) =>
          ErrorsInLocalHistory.localHistoryOpt.foreach { history =>
            history.DivergingImplicitErrors.linkedErrors += ee
          }

//            val text =
//              s"Diverging implicit starting from ${ee.sym}: trying to match an equal or similar (but more complex) type in the same search tree"

          val text = DivergingImplicitErrorView(ee).errMsg

          baseReasons ++ text.split('\n').filter(_.trim.nonEmpty)

        case _ =>
          baseReasons
      }

    }
    (base._1, reasons, base._3)
  }

  object ImplicitErrorTree {

    def fromNode(
        Node: ImplicitError,
        offsprings: List[ImplicitError]
    ): ImplicitErrorTree = {
      val topNesting = Node.nesting

      val children = fromChildren(
        offsprings,
        topNesting
      )

      ImplicitErrorTree(Node, children)
    }

    def fromChildren(
        offsprings: List[ImplicitError],
        topNesting: Int
    ): List[ImplicitErrorTree] = {

      if (offsprings.isEmpty)
        return Nil

      val minNesting = offsprings.map(v => v.nesting).min

      if (minNesting < topNesting + 1)
        throw new SplainInternalError(
          "Detail: nesting level of offsprings of an implicit search tree node should be higher"
        )

      val wII = offsprings.zipWithIndex

      val childrenII = wII
        .filter {
          case (sub, _) =>
            if (sub.nesting < minNesting) {
              throw new SplainInternalError(
                s"Detail: Sub-node in implicit tree can only have nesting level larger than top node," +
                  s" but (${sub.nesting} < $minNesting)"
              )
            }

            sub.nesting == minNesting
        }
        .map(_._2)

      val ranges = {

        val seqs = (childrenII ++ Seq(offsprings.size))
          .sliding(2)
          .toList

        seqs.map {
          case Seq(from, until) =>
            from -> until
          case _ =>
            throw new SplainInternalError("Detail: index should not be empty")
        }
      }

      val children = ranges.map { range =>
        val _top = offsprings(range._1)

        val _offsprings = offsprings.slice(range._1 + 1, range._2)

        fromNode(
          _top,
          _offsprings
        )
      }

      mergeDuplicates(children)
//      children
    }

    def mergeDuplicates(children: List[ImplicitErrorTree]): List[ImplicitErrorTree] = {
      val errors = children.map(_.error).distinct

      val grouped = errors.map { ee =>
        val group = children.filter(c => c.error == ee)

        val mostSpecificError = group.head.error
        // TODO: this old design is based on a huge hypothesis, should it be improved
//        val mostSpecificError = group.map(_.error).maxBy(v => v.candidate.toString.length)

        val allChildren = group.flatMap(v => v.children)
        val mergedChildren = mergeDuplicates(allChildren)

        ImplicitErrorTree(mostSpecificError, mergedChildren)
      }

      grouped.distinctBy(v => v.toString) // TODO: this may lose information
    }
  }

  override def formatImplicitError(
      param: Symbol,
      errors: List[ImplicitError],
      annotationMsg: String
  ): String = {

    val msg = implicitMessage(param, annotationMsg)
    val errorTrees = ImplicitErrorTree
      .fromChildren(errors, -1)

    val errorTreesStr = errorTrees
      .map(_.toString)

    val addendum = errorTrees.headOption.toSeq.flatMap { head =>
      import ImplicitHistory._

      val pos = head.error.candidate.pos
      val localHistoryOpt = currentGlobal.byPosition.get(PositionIndex(pos))
      val addendum: Seq[String] = localHistoryOpt.toSeq.flatMap { history =>
        val unlinkedMsgs = history.DivergingImplicitErrors.getUnlinkedMsgs

        val unlinkedText = if (unlinkedMsgs.nonEmpty) {
          val indented = unlinkedMsgs.flatMap { str =>
            indentTree(List((str, Nil, 0)), 1)
          }

          Seq(
            Messages.WARNING + "The following reported error(s) cannot be linked to any part of the implicit search tree:"
          ) ++
            indented

        } else {
          Nil
        }

        val logs = history.DivergingImplicitErrors.logs

        val logsText = if (logs.nonEmpty) {

          val indented = logs.flatMap { str =>
            indentTree(List((str, Nil, 0)), 1)
          }

          Seq(Messages.WARNING + "Implicit search may be broken:") ++
            indented

        } else {
          Nil
        }

        val text = unlinkedText ++ logsText

        text
      }

      addendum
    }

    val components: Seq[String] =
      Seq("implicit error;") ++
        msg ++
        errorTreesStr ++
        addendum

    val result = components.mkString("\n")

    result
  }
}
