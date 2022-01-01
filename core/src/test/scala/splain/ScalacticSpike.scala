//package splain
//
//import org.scalatest.Ignore
//
//@Ignore
//class ScalacticSpike extends SpecBase {
//
//  it("can diff") {
//
//    val a1 =
//      """
//        |     case (s1: String, s2: String) => StringDiffer.difference(s1, s2, prettifier)
//        |      case (s1: scala.collection.GenMap[Any, Any], s2: scala.collection.GenMap[Any, Any]) => GenMapDiffer.difference(s1, s2, prettifier)
//        |      case (s1: scala.collection.GenSeq[_], s2: scala.collection.GenSeq[_]) => GenSeqDiffer.difference(s1, s2, prettifier)
//        |""".stripMargin
//
//    val a2 =
//      """
//        |     case (s1: String, s2: String) => StringDiffer.difference(s1, s2, prettifier)
//        |      case (s1: scala.collllection.GenMap[Any, Any], s2: scala.collection.GenMap[Any, Any]) => GenMapDiffer.difference(s1, s2, prettifier)
//        |      case (s1: scala.collection.GenSeq[_], s2: scala.collection.GenSeq[_]) => GenSeqDiffer.difference(s1, s2, prettifier)
//        |""".stripMargin
//
//    val vv = PlainPrettifier.apply(a1, a2)
//
//    println(vv)
//  }
//}
