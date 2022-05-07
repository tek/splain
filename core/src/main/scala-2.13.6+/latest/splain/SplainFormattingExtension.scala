package splain

import scala.annotation.tailrec
import scala.tools.nsc.typechecker
import scala.tools.nsc.typechecker.splain.Formatted

object SplainFormattingExtension {

  import scala.reflect.internal.TypeDebugging.AnsiColor._

  val ELLIPSIS: String = "⋮".blue
}

trait SplainFormattingExtension extends typechecker.splain.SplainFormatting with SplainFormattersExtension {
  self: SplainAnalyzer =>

  import SplainFormattingExtension._
  import global._

  case class ImplicitErrorLink(
      fromTree: ImplicitError,
      fromHistory: DivergentImplicitTypeError
  ) {

    val sameCandidateTree: Boolean = fromTree.candidate equalsStructure fromHistory.underlyingTree

    val samePendingType: Boolean = fromTree.specifics match {
      case ss: ImplicitErrorSpecifics.NotFound =>
        fromHistory.pt0 =:= ss.param.tpe
      case _ =>
        false
    }

    val moreSpecificPendingType: Boolean = fromTree.specifics match {
      case ss: ImplicitErrorSpecifics.NotFound =>
        fromHistory.pt0 <:< ss.param.tpe
      case _ =>
        false
    }

    val sameStartingWith: Boolean = {
      fromHistory.sym.fullLocationString == fromTree.candidate.symbol.fullLocationString
    }

    lazy val divergingSearchStartingWithHere: Boolean = sameStartingWith

    lazy val divergingSearchDiscoveredHere: Boolean = sameCandidateTree && moreSpecificPendingType
  }

  object ImplicitErrorLink {}

  case class ImplicitErrorTree(
      error: ImplicitError,
      children: Seq[ImplicitErrorTree] = Nil
  ) {

    import ImplicitErrorTree._

    def doCollectFull(alwaysDisplayRoot: Boolean = false): Seq[ErrorNode] =
      if (children.isEmpty) Seq(ErrorNode(error, alwaysShow = true))
      else {

        Seq(ErrorNode(error, alwaysShow = alwaysDisplayRoot)) ++ {

          if (children.size >= 2) children.flatMap(_.doCollectFull(true))
          else children.flatMap(_.doCollectFull())
        }
      }

    lazy val collectFull: Seq[ErrorNode] = doCollectFull(true)

    lazy val collectCompact: Seq[ErrorNode] = {

      val displayed = collectFull.zipWithIndex.filter {
        case (v, _) =>
          v.alwaysShow
      }

      val ellipsisIndices = displayed.map(_._2 - 1).toSet + (collectFull.size - 1)

      val withEllipsis = displayed.map {
        case (v, i) =>
          if (!ellipsisIndices.contains(i)) v.copy(showEllipsis = true)
          else v
      }

      withEllipsis
    }

    case class FormattedChain(
        source: Seq[ErrorNode]
    ) {

      val toList: List[String] = {
        val collected = source.toList
        val baseIndent = collected.headOption.map(_.nesting).getOrElse(0)

        val formatted = collected.map { v =>
          val formatted = v.formatted
          if (v.showEllipsis) formatted.copy(_2 = formatted._2 :+ ELLIPSIS)
          else formatted
        }

        indentTree(formatted, baseIndent)
      }

      override lazy val toString: String = toList.mkString("\n")
    }

    object FormattedChain {

      object Full extends FormattedChain(collectFull)

      object Compact extends FormattedChain(collectCompact)

      lazy val VimplicitsVerboseTree: Boolean = settings.VimplicitsVerboseTree
      val display: FormattedChain = if (VimplicitsVerboseTree) Full else Compact
    }

    override def toString: String = FormattedChain.Full.toString
  }

  object ImplicitErrorTree {

    case class ErrorNode(
        error: ImplicitError,
        alwaysShow: Boolean,
        showEllipsis: Boolean = false
    ) {

      def nesting: RunId = error.nesting

      val formatted: (String, List[String], RunId) =
        formatNestedImplicit(error)
    }

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

      grouped.distinctBy(v => v.FormattedChain.Full.toString) // TODO: this may lose information
    }
  }

  object ImplicitErrorExtension {
    def unapplyCandidate(e: ImplicitError): Tree = unapplyRecursively(e.candidate)

    @tailrec
    private def unapplyRecursively(tree: Tree): Tree =
      tree match {
        case TypeApply(fun, _) => unapplyRecursively(fun)
        case Apply(fun, _) => unapplyRecursively(fun)
        case a => a
      }

    def cleanCandidate(e: ImplicitError): String =
      unapplyCandidate(e).toString match {
        case ImplicitError.candidateRegex(suf) => suf
        case a => a
      }
  }

  override def formatNestedImplicit(err: ImplicitError): (String, List[String], Int) = {

    val base = super.formatNestedImplicit(err)

    import scala.reflect.internal.TypeDebugging.AnsiColor._
    val candidate = ImplicitErrorExtension.cleanCandidate(err)
    val problem = s"${candidate.red} invalid because"

    object ErrorsInLocalHistory {

      lazy val posI: self.ImplicitHistory.PositionIndex = ImplicitHistory.PositionIndex(
        err.candidate.pos
      )

      lazy val localHistoryOpt: Option[ImplicitHistory.LocalHistory] =
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

          val text = DivergingImplicitErrorView(ee).errMsg

          baseReasons ++ text.split('\n').filter(_.trim.nonEmpty)

        case _ =>
          baseReasons
      }

    }

    (problem, reasons, base._3)
  }

  override def formatImplicitError(
      param: Symbol,
      errors: List[ImplicitError],
      annotationMsg: String
  ): String = {

    val msg = implicitMessage(param, annotationMsg)
    val errorTrees = ImplicitErrorTree.fromChildren(errors, -1)

    val errorTreesStr = errorTrees.map(_.FormattedChain.display.toString)

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

  @deprecated
  override def formatImplicitChainTreeCompact(chain: List[ImplicitError]): Option[List[String]] = {

    throw new SplainInternalError("formatImplicitChainTreeCompact should never be used")
  }

  override def formatDiffImpl(found: Type, req: Type, top: Boolean): Formatted = {
    val (left, right) = dealias(found) -> dealias(req)

    val normalized = Seq(left, right).map(_.normalize).distinct
    if (normalized.size == 1) return formatType(normalized.head, top)

    if (left.typeSymbol == right.typeSymbol) formatDiffInfix(left, right, top)
    else formatDiffSpecial(left, right, top).getOrElse(formatDiffSimple(left, right))
  }
}
