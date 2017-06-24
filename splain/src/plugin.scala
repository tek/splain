package splain
package core

import util.Try
import tools.nsc._
import collection.mutable

trait TypeDiagnostics
// extends typechecker.TypeDiagnostics
extends Formatting
{
  import analyzer._
  import global._

  def featureFoundReq: Boolean

  def echo(msg: String): Unit

  def showStats[A](desc: String, run: => A): A = {
    val ret = run
    if (sys.env.contains("SPLAIN_CACHE_STATS"))
      echo(s"$desc entries/hits: $cacheStats")
    ret
  }

  def foundReqMsgShort(found: Type, req: Type): TypeRepr =
    showStats("foundreq", showFormattedL(formatDiff(found, req, true), true))

  // override def foundReqMsg(found: Type, req: Type): String =
  //   if (featureFoundReq) ";\n" + foundReqMsgShort(found, req).indent.joinLines
  //   else super.foundReqMsg(found, req)
}

// trait Analyzer
// extends typechecker.Analyzer
// with ImplicitChains
// // with ImplicitsCompat
// with TypeDiagnostics
