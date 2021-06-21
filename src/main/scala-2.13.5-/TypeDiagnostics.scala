package splain

import splain.format.{Formatting, TypeRepr}

import scala.tools.nsc._

trait TypeDiagnostics extends typechecker.TypeDiagnostics with Formatting {
  self: Analyzer =>
  import global._

  def featureFoundReq: Boolean

  def showStats[A](desc: String, run: => A): A = {
    val ret = run
    if (sys.env.contains("SPLAIN_CACHE_STATS"))
      typer.context.reporter.echo(s"$desc entries/hits: $cacheStats")
    ret
  }

  def foundReqMsgShort(found: Type, req: Type): TypeRepr =
    showStats("foundreq", showFormattedL(formatDiff(found, req, true), true))

  override def foundReqMsg(found: Type, req: Type): String =
    if (featureFoundReq)
      ";\n" + foundReqMsgShort(found, req).indent.joinLines
    else
      super.foundReqMsg(found, req)
}
