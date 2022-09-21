package splain.test

import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.tools.nsc.Global

class AlwaysError {}

object AlwaysError {

  implicit def summon: AlwaysError = macro Macros.trigger

  class Macros(val c: whitebox.Context) {

    def trigger: c.Tree = {

      val reporter = c.universe.asInstanceOf[Global].reporter

      object CallSite {

        val typer = c.asInstanceOf[scala.reflect.macros.contexts.Context].callsiteTyper

        val ctx = typer.context

        val reporter = ctx.reporter
      }

      val ee = CallSite.reporter.errors

//      CallSite.reporter.clearAllErrors()

//      CallSite.ctx.enclosingContextChain.foreach { cc =>
//        cc.reporter.error(c.enclosingPosition.asInstanceOf[scala.reflect.internal.util.Position], "macro error!")
//      }

//      c.error(c.enclosingPosition, "macro error!")
      c.abort(c.enclosingPosition, "explicit error message!")
      ???
    }
  }
}
