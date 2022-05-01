package splain.test

import scala.collection.immutable.ArraySeq
import scala.reflect.internal.util.{BatchSourceFile, Position}

case class Issue(
    severity: Int,
    msg: String,
    @transient pos: Position,
    sourceName: String = Issue.defaultSrcName,
    isShortName: Boolean = false
) extends Serializable {

  // mimic of PrintReporter.display
  val display: String = {

    val posWithFileName: Position = pos.withSource(
      new BatchSourceFile(sourceName, ArraySeq.unsafeWrapArray(pos.source.content))
    )

    val severityDisplay = new CachingFrontEnd.NoSource.Severity(severity).toString.toLowerCase

    val infoStr = s"$severityDisplay: $msg"

    val formatted = Position.formatMessage(posWithFileName, infoStr, shortenFile = true)
    formatted
  }

  override def toString: String = display
}

object Issue {

  lazy val defaultSrcName = "newSource1.scala"

}
