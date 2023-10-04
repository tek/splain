package splain

import scala.reflect.internal.util.{NoSourceFile, Position}

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

    // probably not useful, withDisambiguation + longString should cover most cases
    lazy val prefixContextIfNeeded: Option[String] = {

      prefixFullName.toLowerCase match {
        case "<root>" | "<empty>" | "<none>" => None
        case _ =>
          if (self.toLongString.startsWith(prefixFullName)) None
          else {
            Some(s"(in $prefixFullName)")
          }
      }
    }

    object _DefPosition {

      lazy val value: Position = definingSymbol.pos

      lazy val noSource: Boolean = value.source == NoSourceFile

      lazy val shortText: String = {

        val prefix = value.source.file.path + ":"

        val result = s"$prefix${value.line}:${value.column}"
        result
      }

      lazy val formattedText: String = {

        Position.formatMessage(value, "", shortenFile = false)
      }
    }

    def defPositionOpt: Option[_DefPosition.type] = Option(_DefPosition).filterNot(_.noSource)

    def typeToString: String = {

      val typeDetail = pluginSettings.typeDetail

      def short = self.safeToString

      def long = scala.util.Try(self.toLongString).getOrElse(short)

      def maybeContext = scala.util.Try(existentialContext(self)).toOption

      def maybeAlias = scala.util.Try(explainAlias(self)).toOption

      typeDetail match {
        case i if i <= 1 => short
        case 2 => long
        case 3 =>
          (Seq(long) ++ maybeContext).mkString("")

        case i if i >= 4 =>
          (Seq(long) ++ maybeContext ++ maybeAlias).mkString("")
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
//    lazy val easilyMistakable: Boolean = {
//
//      val foundWiden = found.widen
//      val reqWiden = req.widen
//      val sameNamesDifferentPrefixes =
//        foundWiden.typeSymbol.name == reqWiden.typeSymbol.name &&
//          foundWiden.prefix.typeSymbol != reqWiden.prefix.typeSymbol
//      val easilyMistakable =
//        sameNamesDifferentPrefixes &&
//          !req.typeSymbol.isConstant &&
//          finalOwners(foundWiden) && finalOwners(reqWiden) &&
//          !found.typeSymbol.isTypeParameterOrSkolem && !req.typeSymbol.isTypeParameterOrSkolem
//
//      easilyMistakable
//    }

    lazy val builtInDiffMsg: String = {
      val result = builtinFoundReqMsg(found, req)
      result
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
