package splain.testing

import splain.testing.static.{Staged, StaticAnalysisMacros}

import scala.language.experimental.macros

object StaticAnalysis {

  trait Settings {}

  object Settings {

    object Typer extends Settings {}

    object Parser extends Settings {}
  }

  object SourceFile {

    final def apply[T](tree: T): Staged =
      macro StaticAnalysisMacros.codeBlock[Settings.Typer.type]
  }

  object SourceBlock {

    final def apply[T](tree: T): Staged =
      macro StaticAnalysisMacros.codeBlock[Settings.Typer.type]
  }

  object SourceLiteral {

    final def apply(code: String): Staged =
      macro StaticAnalysisMacros.codeLiteral[Settings.Typer.type]
  }

}
