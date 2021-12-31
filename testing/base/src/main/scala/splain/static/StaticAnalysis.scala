package splain.static

import splain.runtime.TryCompile

import scala.language.experimental.macros

object StaticAnalysis {

  object SourceBlock {

    final def apply[T, ARGS <: String](tree: T, args: ARGS): TryCompile =
      macro StaticAnalysisMacros.codeBlock[ARGS]
  }

  object SourceLiteral {

    final def apply[LIT <: String, ARGS <: String](lit: LIT, args: ARGS): TryCompile =
      macro StaticAnalysisMacros.codeLiteral[ARGS]
  }
}
