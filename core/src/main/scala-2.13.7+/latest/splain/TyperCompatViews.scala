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

      val ttString = TypeView(noArgType).typeToString

      if (ttString.startsWith(ownerPathPrefix)) {
        ownerPath -> ttString.stripPrefix(ownerPathPrefix).stripPrefix(".")
      } else {
        Nil -> ttString
      }
    }

    lazy val prefixFullName: String = {
      self.prefix.typeSymbol.fullNameString
    }

    lazy val prefixContext: String = { s"in $prefixFullName" }

    lazy val typeToString: String = {

      val details = pluginSettings.typeDetails.getOrElse(1)

      lazy val short = self.safeToString
      lazy val long = scala.util.Try(self.toLongString).getOrElse(short)

      lazy val ec = existentialContext(self)

      lazy val pc =
        if (long.startsWith(prefixFullName)) ""
        else {
          s" {$prefixContext}"
        }

      lazy val withEc = scala.util
        .Try(
          long + ec
        )
        .getOrElse(long)

      lazy val withPcEc = scala.util
        .Try(
          long + pc + ec
        )
        .getOrElse(withEc)

      if (details <= 1) {
        short
      } else if (details == 2) {
        long
      } else if (details == 3) {
        withEc
      } else {
        withPcEc
      }
    }
  }

  case class TypeDiffView(
      found: Type,
      req: Type
  ) {

    def map(fn: Type => Type): TypeDiffView =
      TypeDiffView(fn(found), fn(req))

    def toTuple[T](fn: Type => T): (T, T) = (fn(found), fn(req))

    // copied from eponymous variable in Scala compiler
    // apparently doesn't work after type arg stripped
    lazy val easilyMistakable: Boolean = {

      val foundWiden = found.widen
      val reqWiden = req.widen
      val sameNamesDifferentPrefixes =
        foundWiden.typeSymbol.name == reqWiden.typeSymbol.name &&
          foundWiden.prefix.typeSymbol != reqWiden.prefix.typeSymbol
      val easilyMistakable =
        sameNamesDifferentPrefixes &&
          !req.typeSymbol.isConstant &&
          finalOwners(foundWiden) && finalOwners(reqWiden) &&
          !found.typeSymbol.isTypeParameterOrSkolem && !req.typeSymbol.isTypeParameterOrSkolem

      easilyMistakable
    }
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
