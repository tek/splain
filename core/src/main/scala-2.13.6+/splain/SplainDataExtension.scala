package splain

import scala.tools.nsc.typechecker

trait SplainDataExtension extends typechecker.splain.SplainData with typechecker.splain.SplainErrors {
  self: SplainAnalyzer =>

  import global._

//  case class Divergent(param: Symbol) extends ImplicitErrorSpecifics

//  def splainPushOrReportDivergent(tree: Tree, param: Symbol, annotationMsg: String): String = {
//
//    if (settings.Vimplicits)
//      if (ImplicitErrors.nested) {
//        splainPushNotFound(tree, param)
//        ""
//      } else
//        pluginsNoImplicitFoundError(
//          param,
//          ImplicitErrors.errors,
//          formatImplicitError(param, ImplicitErrors.errors, annotationMsg)
//        )
//    else ""
//  }
}
