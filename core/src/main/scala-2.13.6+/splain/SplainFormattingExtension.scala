package splain

import scala.tools.nsc.typechecker

trait SplainFormattingExtension extends typechecker.splain.SplainFormatting with SplainFormattersExtension {
  self: SplainAnalyzer =>

  import global._

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

    lazy val ii = ImplicitSession.PositionIndex(
      err.candidate.pos
//      err.candidate.symbol
    )

    object AnnotatedErrors {

      lazy val diverging: Seq[DivergentImplicitTypeError] = {
        val vsOpt = ImplicitSession.current.Diverging.byPosition.get(ii)
        vsOpt.toSeq.flatten
      }
    }

    val reason = err.specifics match {
      case _err: ImplicitErrorSpecifics.NotFound =>
        val annotation = NoImplicitFoundAnnotation(err.candidate, _err.param)._2
        val base = implicitMessage(_err.param, annotation)

        val regardingSameMissingType = AnnotatedErrors.diverging.find { ee =>
          ee.pt0 =:= _err.param.tpe
        }

        regardingSameMissingType match {
          case Some(ee) =>
            base ++
              Seq(
                s"Diverging implicit starting from ${ee.sym}: trying to match an equal or similar (but more complex) type in the same search tree"
              ).filter(_.trim.nonEmpty)

          case _ =>
            base
        }

      case e: ImplicitErrorSpecifics.NonconformantBounds => formatNonConfBounds(e)
    }
    (base._1, reason, base._3)
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

      val distinctChildren = {
        children.distinctBy { child =>
          child.error
        }
      }

      distinctChildren
    }
  }

  override def formatImplicitError(
      param: Symbol,
      errors: List[ImplicitError],
      annotationMsg: String
  ): String = {

    val treeNodes = ImplicitErrorTree.fromChildren(errors, -1)

    val msg = implicitMessage(param, annotationMsg)

    val components =
      Seq("implicit error;") ++
        implicitMessage(param, annotationMsg) ++
        treeNodes

    val result = components.mkString("\n")

    result
  }
}
