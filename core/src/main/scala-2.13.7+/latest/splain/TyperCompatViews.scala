package splain

trait TyperCompatViews {
  self: SplainAnalyzer =>

  import global._

  case class TypeView(self: Type) {

    lazy val extractArgs: List[global.Type] = self match {
      // PolyType handling is removed for being unsound
      case t: AliasTypeRef if !isAux(self) =>
        t.betaReduce.typeArgs.map(a => if (a.typeSymbolDirect.isTypeParameter) WildcardType else a)
      case _ => self.typeArgs
    }

    lazy val noArgType: Type = if (extractArgs.nonEmpty) {
      self.typeConstructor
    } else {
      self
    }

    lazy val definingSymbol: Symbol = {

      self match {
        case tt: SingletonType =>
          tt.termSymbolDirect
        case _ =>
          self.typeSymbolDirect
      }
    }

    private lazy val parts = definingSymbol.ownerChain.reverse
      .map(_.name.decodedName.toString)
      .filterNot(part => part.startsWith("<") && part.endsWith(">"))

    lazy val (path, noArgShortName) = {

      val (ownerPath, _) = parts.splitAt(Math.max(0, parts.size - 1))

      val ownerPathPrefix = ownerPath.mkString(".")

      val ttString = noArgType.safeToString
//      val ttString = TypeView(noArgType).safeToLongString

      if (ttString.startsWith(ownerPathPrefix)) {
        ownerPath -> ttString.stripPrefix(ownerPathPrefix).stripPrefix(".")
      } else {
        Nil -> ttString
      }
    }

    lazy val safeToLongString: String = {
      try {
        self.toLongString
      } catch {
        case _: Exception =>
          self.safeToString
      }
    }

    lazy val extraRationale: String = existentialContext(self) + explainAlias(self)
  }

  case class DivergingImplicitErrorView(self: DivergentImplicitTypeError) {

    lazy val errMsg: String = {

      val formattedPT = showFormatted(formatType(self.pt0, top = false))

      s"diverging implicit expansion for type $formattedPT\nstarting with ${self.sym.fullLocationString}"
    }
  }

  case class PositionIndex(
      pos: Position
  ) {}

}
