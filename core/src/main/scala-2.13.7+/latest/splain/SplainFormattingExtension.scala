package splain

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.typechecker
import scala.tools.nsc.typechecker.splain._

object SplainFormattingExtension {

  import scala.reflect.internal.TypeDebugging.AnsiColor._

  val ELLIPSIS: String = "⋮".blue
}

trait SplainFormattingExtension extends typechecker.splain.SplainFormatting with SplainFormattersExtension {
  self: SplainAnalyzer =>

  import SplainFormattingExtension._
  import global._
  import PluginSettings._

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

    //    lazy val divergingSearchStartingWithHere: Boolean = sameStartingWith

    lazy val divergingSearchDiscoveredHere: Boolean = sameCandidateTree && moreSpecificPendingType
  }

  case class SplainImplicitErrorTree(
      error: ImplicitError,
      children: Seq[SplainImplicitErrorTree] = Nil
  ) {

    import SplainImplicitErrorTree._

    def doCollectFull(alwaysDisplayRoot: Boolean = false): Seq[NodeForShow] = {

      if (children.isEmpty) Seq(NodeForShow(error, alwaysShow = true))
      else {

        Seq(NodeForShow(error, alwaysShow = alwaysDisplayRoot)) ++ {

          if (children.size >= 2) children.flatMap(_.doCollectFull(true))
          else children.flatMap(_.doCollectFull())
        }
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

    object ImplicitErrorsInHistory {

      lazy val posI: self.PositionIndex = PositionIndex(
        err.candidate.pos
      )

      lazy val localHistoryOpt: Option[ImplicitsHistory.Local] =
        ImplicitsHistory.currentGlobal.localByPosition.get(posI)

      lazy val diverging: Seq[DivergentImplicitTypeError] = {

        localHistoryOpt.toSeq.flatMap { history =>
          history.DivergingImplicitErrors.errors
        }
      }
    }

    val extra = mutable.Buffer.empty[String]

    extra ++= base._2

    val discoveredHere = ImplicitErrorsInHistory.diverging.find { inHistory =>
      val link = SplainImplicitErrorLink(err, inHistory)

      link.divergingSearchDiscoveredHere
    }

    discoveredHere match {
      case Some(ee) =>
        ImplicitErrorsInHistory.localHistoryOpt.foreach { history =>
          history.DivergingImplicitErrors.linkedErrors += ee
        }

        val text = DivergingImplicitErrorView(ee).errMsg

        extra ++= text.split('\n').filter(_.trim.nonEmpty)
      case _ =>
    }

    (problem, extra.toList, base._3)
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

  override def formatImplicitError(
      param: Symbol,
      errors: List[ImplicitError],
      annotationMsg: String
  ): String = {

    val msg = implicitMessage(param, annotationMsg)
    val errorTrees = SplainImplicitErrorTree.fromChildren(errors, -1)

    val errorTreesStr = errorTrees.map(_.FormattedChain.display.toString)

    val addendum = errorTrees.headOption.toSeq.flatMap { head =>
      import ImplicitsHistory._

      val pos = head.error.candidate.pos
      val localHistoryOpt = currentGlobal.localByPosition.get(PositionIndex(pos))
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

  override def extractArgs(tpe: Type): List[global.Type] = TypeView(tpe).extractArgs

  override def stripType(tt: Type): (List[String], String) = {

    val view = TypeView(tt)
    view.path -> view.noArgShortName
  }

  // new implementation is idempotent and won't lose information
  override def dealias(tpe: Type): Type = {

    TypeView(tpe).dealias_normal
  }

  case class FormattedIndex(
      ft: Formatted,
      idHash: Int
  )

  object FormattedIndex {

    def apply(ft: Formatted) = new FormattedIndex(
      ft,
      System.identityHashCode(ft)
    )
  }

  trait Based {

    def element: Formatted

    protected def formattedHeader_Body(break: Boolean): (String, Seq[TypeRepr])

    lazy val flat: String = {
      val (header, body) = formattedHeader_Body(false)

      if (body.isEmpty) header
      else s"$header { ${body.map(v => v.flat).mkString(";")} }"
    }

    lazy val broken: Seq[String] = {
      val (header, body) = formattedHeader_Body(true)
      val result = indentTree(List((header, body.flatMap(_.lines).toList, 0)), 1)

      result
    }
  }

  object Based {

    lazy val lookup: TrieMap[FormattedIndex, ArrayBuffer[Based]] = TrieMap.empty

    // MultiMap shortcuts
    def +=(kv: (FormattedIndex, Based)): Unit = {

      lookup.getOrElseUpdate(kv._1, ArrayBuffer.empty) += kv._2
    }

    def getAll(k: FormattedIndex): List[Based] = lookup.get(k).toList.flatten
  }

  case class Reduction(
      element: Formatted,
      from: (String, Formatted)
  ) extends Based {

    def index(): Unit = {

      if (TypeDetail.reduction.isEnabled) {
        Based += FormattedIndex(element) -> this
      }
    }

    override protected def formattedHeader_Body(break: Boolean): (String, Seq[TypeRepr]) = {

      s"(${from._1})" -> Seq(showFormattedLImpl(from._2, break))
    }
  }

  case class BuiltInDiffMsg(
      element: Formatted,
      msg: String,
      infixOpt: Option[Formatted] = None
  ) extends Based {

    def index(): Unit = {

      if (TypeDiffsDetail.`builtin-msg`.isEnabled)
        Based += FormattedIndex(element) -> this
    }

    override protected def formattedHeader_Body(break: Boolean): (String, Seq[TypeRepr]) = {

      lazy val infixText = infixOpt match {
        case None => "|"
        case Some(ii) => " " + showFormattedLImpl(ii, break).flat + " "
      }

      val indented = msg
        .split("\n")
        .filter(_ != ";")

      s"(comparing <found>$infixText<required>)" -> indented.map(v => FlatType(v))
    }
  }

  case class DefPosition(
      element: Formatted,
      srcInfo: String,
      quotes: Seq[String] = Nil
  ) extends Based {

    def index(): Unit = {

      if (TypeDetail.position.isEnabled)
        Based += FormattedIndex(element) -> this
    }

    override protected def formattedHeader_Body(break: Boolean): (String, Seq[TypeRepr]) = {

      s"(defined at $srcInfo)" -> quotes.map(quote => BrokenType(List(quote)))
    }
  }

  def formatTypeRaw(tpe: Type, top: Boolean): Formatted = {
    formatWithInfix(tpe, extractArgs(tpe), top)(formatType)
  }

  override def formatTypeImpl(tpe: Type, top: Boolean): Formatted = {

    tpe.typeArgs match {
      case List(t1, t2) =>
        val result =
          if (TypeDiffsDetail.disambiguation.isEnabled) {

            withDisambiguation(Nil, t1, t2) {
              formatTypeImplNoDisambiguation(tpe, top)
            }
          } else {

            formatTypeImplNoDisambiguation(tpe, top)
          }

        result match {
          case Infix(ii, left, right, _) =>
            val noApparentDiff = (left == right) && (t1 != t2)

            if (noApparentDiff || TypeDiffsDetail.`builtin-msg-always`.isEnabled) {

              BuiltInDiffMsg(
                result,
                TypeDiffView(t1, t2).builtInDiffMsg,
                Some(ii)
              ).index()
            }
          case _ =>
        }

        result
      case _ =>
        formatTypeImplNoDisambiguation(tpe, top)
    }

  }

  protected def formatTypeImplNoDisambiguation(tpe: Type, top: Boolean): Formatted = {
    val dtpe = dealias(tpe)

    val results = Seq(tpe, dtpe).distinct.map { t =>
      formatTypeRaw(t, top)
    }.distinct

    results match {
      case Seq(from, reduced) =>
        Reduction(reduced, "reduced from" -> from).index()
      case _ =>
    }

    val result = results.last

    TypeView(tpe).defPositionOpt.foreach { v =>
      DefPosition(
        result,
        v.shortText
      ).index()
    }

    result
  }

  override def formatDiffImpl(found: Type, req: Type, top: Boolean): Formatted = {

    if (TypeDiffsDetail.disambiguation.isEnabled) {

      val result = withDisambiguation(Nil, found, req) {
        formatDiffImplNoDisambiguation(found, req, top)
      }

      result match {
        case diff: Diff =>
          val noApparentDiff = (diff.left == diff.right) && (found != req)

          if (noApparentDiff || TypeDiffsDetail.`builtin-msg-always`.isEnabled) {

            BuiltInDiffMsg(
              diff,
              TypeDiffView(found, req).builtInDiffMsg
            ).index()
          }
        case _ =>
      }

      result
    } else {

      formatDiffImplNoDisambiguation(found, req, top)
    }
  }

  protected def formatDiffImplNoDisambiguation(found: Type, req: Type, top: Boolean): Formatted = {

    val reduced = Seq(found, req).map(dealias)
    val Seq(left, right) = reduced

    if (reduced.distinct.size == 1) {
      val only = reduced.head
      val result = formatType(only, top)

      val basedOn = Seq(found, req).distinct
        .map { tt =>
          formatTypeRaw(tt, top)
        }
        .distinct
        .flatMap { ft =>
          if (ft == result) None
          else Some("normalized from" -> ft)
        }

      basedOn.foreach { v =>
        Reduction(
          result,
          v
        ).index()
      }

      result
    } else {
      val result = {
        val noArgs = Seq(left, right).map { tt =>
          TypeView(tt).noArgType
        }

        if (noArgs.distinct.size == 1) {
          formatDiffInfix(left, right, top)
        } else {
          formatDiffSpecial(left, right, top).getOrElse {

            val result = formatDiffSimple(left, right)
            result
          }
        }
      }

      val basedOn = Seq(
        "left side reduced from" -> Seq(found, left),
        "right side reduced from" -> Seq(req, right)
      )
        .flatMap {
          case (clause, fts) =>
            if (fts.distinct.size == 1) None
            else {
              val formatted = fts.map { ft =>
                formatTypeRaw(ft, top)
              }.distinct
              if (formatted.size == 1) None
              else Some(clause -> formatted.head)
            }
        }

      basedOn.foreach { v =>
        Reduction(
          result,
          v
        ).index()
      }

      result
    }
  }

  override def showFormattedLImpl(ft: Formatted, break: Boolean): TypeRepr = {

    def appendLastLine(lines: List[String], suffix: String): List[String] = {
      lines match {
        case Nil => List(suffix)
        case head :: Nil => List(head + suffix)
        case head :: tail => head :: appendLastLine(tail, suffix)
      }
    }

    /**
      * If the args of an applied type constructor are multiline, create separate lines for the constructor name and the
      * closing bracket; else return a single line.
      */
    def showEnclosed(
        base: TypeRepr,
        args: List[TypeRepr],
        brackets: (String, String) = "[" -> "]",
        splitter: String = ","
    ): TypeRepr = {
      val flatArgs = args.map(_.flat).mkString(brackets._1, splitter + " ", brackets._2)
      val flat = FlatType(s"${base.flat}$flatArgs")

      def brokenArgs = args match {
        case head :: tail => tail.foldLeft(head.lines)((z, a) => appendLastLine(z, splitter) ::: a.lines)
        case _ => Nil
      }

      def broken = BrokenType(appendLastLine(base.lines, brackets._1) ::: indent(brokenArgs) ::: List(brackets._2))

      if (break) decideBreak(flat, broken) else flat
    }

    def showCompound(
        types: List[TypeRepr],
        infixText: String = "with"
    ): TypeRepr = {
      val infixWithSpace = s" $infixText "

      def flat = FlatType(types.map(_.flat).mkString(infixWithSpace))
      def broken = BrokenType(
        types.map(_.lines).reduceLeft((z, a) => appendLastLine(z, infixWithSpace) ::: a)
      )

      if (break) decideBreak(flat, broken) else flat
    }

    def _showFormattedL(v: Formatted) = showFormattedL(v, break)

    val raw = ft match {
      case Simple(name) => FlatType(name.name)
      case Qualified(path, name) => showFormattedQualified(path, name)
      case Applied(cons, args) => showEnclosed(_showFormattedL(cons), args.map(_showFormattedL))
      case tpe @ Infix(_, _, _, top) =>
        wrapParensRepr(
          if (break) breakInfix(flattenInfix(tpe)) else FlatType(flattenInfix(tpe).map(showFormatted).mkString(" ")),
          top
        )
      case UnitForm => FlatType("Unit")
      case FunctionForm(args, ret, top) =>
        FlatType(wrapParens(s"${showFuncParams(args.map(showFormatted))} => ${showFormatted(ret)}", top))
      case TupleForm(elems) =>
        val formattedElems = elems.map(_showFormattedL)

        if (elems.size == 1) {
          FlatType(showTuple(formattedElems.map(_.flat))) // TODO: the name showTuple is obsolete
        } else {
          showEnclosed(FlatType(""), formattedElems, brackets = "(" -> ")")
        }

      case RefinedForm(elems, decls) =>
        val compound = showCompound(elems.map(_showFormattedL))

        val refined =
          if (decls.isEmpty)
            compound
          else if (truncateDecls(decls))
            showEnclosed(compound, List(FlatType("...")), brackets = " {" -> "}", splitter = ";")
          else
            showEnclosed(compound, decls.map(_showFormattedL), brackets = " {" -> "}", splitter = ";")

        refined

      case Diff(left, right) => FlatType(formattedDiff(left, right))
      case Decl(sym, rhs) => FlatType(s"type ${showFormatted(sym)} = ${showFormatted(rhs)}")
      case DeclDiff(sym, left, right) => FlatType(s"type ${showFormatted(sym)} = ${formattedDiff(left, right)}")
      case ByName(tpe) => FlatType(s"(=> ${showFormatted(tpe)})")
    }

    val index = FormattedIndex(ft)
    val basedOn = Based.getAll(index)

    val result = {

      basedOn match {
        case Nil =>
          raw
        case _ =>
          def flat = FlatType(raw.flat + " " + basedOn.map(_.flat).mkString(" "))
          def broken = BrokenType(raw.lines ++ basedOn.flatMap(_.broken))

          raw match {
            case _: BrokenType => broken
            case _ if !break => flat
            case _ => decideBreak(flat, broken)
          }
      }
    }

    result
  }
}
