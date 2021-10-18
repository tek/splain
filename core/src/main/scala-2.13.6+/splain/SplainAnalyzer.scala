package splain

import scala.tools.nsc._

class SplainAnalyzer(val global: Global) extends typechecker.Analyzer with SplainFormattingExtension {

  import global._

  override val specialFormatters: List[SpecialFormatter] =
    List(
      FunctionFormatter,
      TupleFormatter,
      SLRecordItemFormatter,
      RefinedFormatterImproved,
//      RefinedFormatter,
      ByNameFormatter
    )

//  override def NoImplicitFoundError(tree: Tree, param: Symbol)(
//      implicit
//      context: Context
//  ): Unit = {
//    val (isSupplement, annotationMsg) = NoImplicitFoundAnnotation(tree, param)
//    def defaultErrMsg = {
//      val paramName = param.name
//      val paramTp = param.tpe
//      def evOrParam =
//        if (paramName startsWith nme.EVIDENCE_PARAM_PREFIX)
//          "evidence parameter of type"
//        else
//          s"parameter $paramName:"
//      if (annotationMsg == null || annotationMsg.isEmpty)
//        s"could not find implicit value for $evOrParam $paramTp$annotationMsg"
//      else annotationMsg
//    }
//    val errMsg = splainPushOrReportNotFound(tree, param, annotationMsg)
//    ErrorUtils.issueNormalTypeError(tree, if (errMsg.isEmpty) defaultErrMsg else errMsg)
//  }
//
//  override def splainPushOrReportNotFound(tree: Tree, param: Symbol, annotationMsg: String): String = {
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
