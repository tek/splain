package splain

import scala.annotation.tailrec
import scala.collection.mutable
import scala.tools.nsc.typechecker
import scala.tools.nsc.typechecker.splain.{Applied, Formatted, Qualified, SimpleName}

object SplainFormattingExtension {

  import scala.reflect.internal.TypeDebugging.AnsiColor._

  val ELLIPSIS: String = "⋮".blue
}

trait SplainFormattingExtension extends typechecker.splain.SplainFormatting with SplainFormattersExtension {
  self: SplainAnalyzer =>

  import SplainFormattingExtension._
  import global._

  case class SplainImplicitErrorLink(
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

  def safeToLongString(tt: Type): String = {
    try {
      tt.toLongString
    } catch {
      case _: Exception =>
        tt.safeToString
    }
  }

  trait TypeAddendum {

    def isInformative: Boolean
  }

  object TypeAddendum {

    case class Reduction(
        tt: Type
    ) extends TypeAddendum {

      lazy val additionalInfo: String = existentialContext(tt) + explainAlias(tt)

      override def isInformative: Boolean = additionalInfo.nonEmpty

      lazy val info: String = {
        safeToLongString(tt) + additionalInfo
      }

      override lazy val toString: String = "(* " + info + " )"
    }
  }

  case class SplainImplicitErrorTree(
      error: ImplicitError,
      children: Seq[SplainImplicitErrorTree] = Nil
  ) {

    import SplainImplicitErrorTree._

    def doCollectFull(alwaysDisplayRoot: Boolean = false): Seq[NodeForShow] =
      if (children.isEmpty) Seq(NodeForShow(error, alwaysShow = true))
      else {

        Seq(NodeForShow(error, alwaysShow = alwaysDisplayRoot)) ++ {

          if (children.size >= 2) children.flatMap(_.doCollectFull(true))
          else children.flatMap(_.doCollectFull())
        }
      }

    lazy val collectFull: Seq[NodeForShow] = doCollectFull(true)

    lazy val collectCompact: Seq[NodeForShow] = {

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
        source: Seq[NodeForShow]
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

      lazy val VimplicitsVerboseTree: Boolean = settings.VimplicitsVerboseTree.value
      val display: FormattedChain = if (VimplicitsVerboseTree) Full else Compact
    }

    override def toString: String = FormattedChain.Full.toString
  }

  object SplainImplicitErrorTree {

    case class NodeForShow(
        error: ImplicitError,
        alwaysShow: Boolean,
        showEllipsis: Boolean = false
    ) {

      def nesting: RunId = error.nesting

      val formatted: (String, List[String], RunId) =
        formatNestedImplicit(error)
    }

    def fromError(
        error: ImplicitError,
        offsprings: List[ImplicitError]
    ): SplainImplicitErrorTree = {
      val topNesting = error.nesting

      val children = fromChildren(
        offsprings,
        topNesting
      )

      val addenda = mutable.Buffer.empty[TypeAddendum]

      if (pluginSettings.typeReduction)
        addenda += TypeAddendum.Reduction(error.tpe)

      SplainImplicitErrorTree(error, children)
    }

    def fromChildren(
        offsprings: List[ImplicitError],
        topNesting: Int
    ): List[SplainImplicitErrorTree] = {

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

        fromError(
          _top,
          _offsprings
        )
      }

      mergeDuplicates(children)
      //      children
    }

    def mergeDuplicates(children: List[SplainImplicitErrorTree]): List[SplainImplicitErrorTree] = {
      val errors = children.map(_.error).distinct

      val grouped = errors.map { ee =>
        val group = children.filter(c => c.error == ee)

        val mostSpecificError = group.head.error
        // TODO: this old design is based on a huge hypothesis, should it be improved
        //        val mostSpecificError = group.map(_.error).maxBy(v => v.candidate.toString.length)

        val allChildren = group.flatMap(v => v.children)
        val mergedChildren = mergeDuplicates(allChildren)

        SplainImplicitErrorTree(mostSpecificError, mergedChildren)
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

    val reasons = mutable.Buffer.empty[String]

    reasons ++= base._2

    val discoveredHere = ErrorsInLocalHistory.diverging.find { inHistory =>
      val link = SplainImplicitErrorLink(err, inHistory)

      link.divergingSearchDiscoveredHere
    }

    discoveredHere match {
      case Some(ee) =>
        ErrorsInLocalHistory.localHistoryOpt.foreach { history =>
          history.DivergingImplicitErrors.linkedErrors += ee
        }

        val text = DivergingImplicitErrorView(ee).errMsg

        reasons ++= text.split('\n').filter(_.trim.nonEmpty)
      case _ =>
    }

    if (pluginSettings.typeReduction) {
      val reduction = TypeAddendum.Reduction(err.tpe)
      if (reduction.isInformative) {
        reasons += reduction.toString
      }
    }

    (problem, reasons.toList, base._3)
  }

  override def formatWithInfix[A](tpe: Type, args: List[A], top: Boolean)(rec: (A, Boolean) => Formatted): Formatted = {
    val (path, simple) = formatSimpleType(tpe)
    tpe.nameAndArgsString
    lazy val formattedArgs = args.map(rec(_, true))
    val special = formatSpecial(tpe, simple, args, formattedArgs, top)(rec)
    special.getOrElse {
      args match {
        case left :: right :: Nil if isSymbolic(tpe) => formatInfix(path, simple, left, right, top)(rec)
        case _ :: _ => Applied(Qualified(path, SimpleName(simple)), formattedArgs)
        case _ => Qualified(path, SimpleName(simple))
      }
    }
  }

//  override def formatTypeImpl(tpe: Type, top: Boolean): Formatted = {
//    val dTpe = dealias(tpe)
//    val args = extractArgs(dTpe)
//    val result = formatWithInfix(dTpe, args, top)(formatType)
//    result
//  }

  // TODO: merge into TypeView
  override def stripType(tt: Type): (List[String], String) = {
    if (extractArgs(tt).nonEmpty) { // TODO: redundant resolving

      val ttc = tt.typeConstructor
      // TODO: should this also use TypeParts ?
      val sym = tt.typeSymbolDirect
      //      val sym = if (tpe.takesTypeArgs) tpe.typeSymbolDirect else tpe.typeSymbol
      val parts = TypeView(sym, ttc)
      //      val symName = sym.name.decodedName.toString
      (parts.path, parts.shortName)
    } else {

      val parts = TypeView(tt.termSymbolDirect, tt)
      parts.path -> parts.shortName
    }

//    tt match {
//      case tt: SingletonType =>
//        val parts = TypeView(tt.termSymbolDirect, tt)
//        parts.path -> parts.shortName
//
//      case tt: RefinedType =>
//        val parts = TypeView(tt.typeSymbolDirect, tt)
//        parts.path -> parts.shortName
//
//      case tt: ExistentialType =>
//        val parts = TypeView(tt.typeSymbolDirect, tt)
//        parts.path -> parts.shortName
//
//      case _ =>
//        val ttc = tt.typeConstructor
//        // TODO: should this also use TypeParts ?
//        val sym = tt.typeSymbolDirect
//        //      val sym = if (tpe.takesTypeArgs) tpe.typeSymbolDirect else tpe.typeSymbol
//        val parts = TypeView(sym, ttc)
//        //      val symName = sym.name.decodedName.toString
//        (parts.path, parts.shortName)
//    }
  }

  case class TypeView(sym: Symbol, tt: Type) {

    private lazy val parts = sym.ownerChain.reverse
      .map(_.name.decodedName.toString)
      .filterNot(part => part.startsWith("<") && part.endsWith(">"))

    lazy val (path, shortName) = {

      val (ownerPath, _) = parts.splitAt(Math.max(0, parts.size - 1))

      val ownerPathPrefix = ownerPath.mkString(".")

      val ttString = tt.safeToString

      if (ttString.startsWith(ownerPathPrefix)) {
        ownerPath -> ttString.stripPrefix(ownerPathPrefix).stripPrefix(".")
      } else {
        Nil -> ttString
      }
    }
  }

  override def formatImplicitError(
      param: Symbol,
      errors: List[ImplicitError],
      annotationMsg: String
  ): String = {

    val msg = implicitMessage(param, annotationMsg)
    val errorTrees = SplainImplicitErrorTree.fromChildren(errors, -1)

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

//  override def formatDiffImpl(found: Type, req: Type, top: Boolean): Formatted = {
//    val (left, right) = dealias(found) -> dealias(req)
//
//    val normalized = Seq(left, right).map(_.normalize).distinct
//
//    val result = {
//      if (normalized.size == 1) formatType(normalized.head, top)
//      else if (left.typeSymbolDirect == right.typeSymbolDirect)
//        formatDiffInfix(left, right, top)
//      else
//        formatDiffSpecial(left, right, top).getOrElse(
//          formatDiffSimple(left, right)
//        )
//    }
//
//    result
//  }

  override def splainFoundReqMsg(found: Type, req: Type): String = {
    val body =
      if (settings.VtypeDiffs.value)
        ";\n" + showFormattedL(formatDiff(found, req, top = true), break = true).indent.joinLines
      else ""

    val addenda = if (pluginSettings.typeReduction) {
      Seq(found, req)
        .map { tt =>
          TypeAddendum.Reduction(tt)
        }
        .filter(_.isInformative)
    } else {
      Nil
    }

    (Seq(body) ++ addenda).mkString("\n")
  }

  // new implementation is idempotent and won't lose information
  override def dealias(tpe: Type): Type = {

    if (isAux(tpe)) tpe
    else tpe.dealias
  }

  override def extractArgs(tpe: Type): List[global.Type] = tpe match {
    // PolyType handling is removed for being unsound
    case t: AliasTypeRef if !isAux(tpe) =>
      t.betaReduce.typeArgs.map(a => if (a.typeSymbolDirect.isTypeParameter) WildcardType else a)
    case _ => tpe.typeArgs
  }
}
