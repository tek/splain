package splain

import scala.tools.nsc.typechecker

trait SplainFormattingExtension extends typechecker.splain.SplainFormatting {
  self: SplainAnalyzer =>

  import global._

  case class ImplicitErrorTree(
      error: ImplicitError,
      children: Seq[ImplicitErrorTree] = Nil
  ) {

    lazy val asChain: List[ImplicitError] = List(error) ++ children.flatMap(_.asChain)

    override def toString: String = {
      formatImplicitChain(asChain).mkString("\n")
    }
  }

  object ImplicitErrorTree {

    def fromNode(
        Node: ImplicitError,
        offsprings: List[ImplicitError]
    ): ImplicitErrorTree = {
      val topNesting = Node.nesting

      val children = fromChildren(
        offsprings,
        topNesting + 1
      )

      ImplicitErrorTree(Node, children)
    }

    def fromChildren(
        offsprings: List[ImplicitError],
        minNesting: Int
    ): List[ImplicitErrorTree] = {

      if (offsprings.isEmpty)
        return Nil

      val wII = offsprings.zipWithIndex

      val childrenII = wII
        .filter {
          case (sub, _) =>
            require(
              sub.nesting >= minNesting,
              s"Sub-node in implicit tree can only have nesting level larger than top node," +
                s" but (${sub.nesting} < $minNesting)"
            )
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
            throw new SplainInternalError(
              "You've found a bug in splain formatting extension," +
                " please post this error with stack trace on https://github.com/tek/splain/issues"
            )
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

    val treeNodes = ImplicitErrorTree.fromChildren(errors, 0)

    val result =
      s"""
         |implicit error;
         |${(implicitMessage(param, annotationMsg) ++ treeNodes).mkString("\n\n")}
         |""".stripMargin.trim

    result
  }
}
