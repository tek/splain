package splain.runtime

import scala.collection.immutable.ArraySeq
import scala.reflect.internal.util.{BatchSourceFile, Position}

case class Issue(
    level: Int,
    msg: String,
    pos: Position,
    sourceName: String = "newSource1.scala",
    isShortName: Boolean = false
) {

  // mimic of PrintReporter.display
  def display: String = {

    val posWithFileName: Position = pos.withSource(
      new BatchSourceFile(sourceName, ArraySeq.unsafeWrapArray(pos.source.content))
    )

    val levelDisplay = new StoreFrontEnd.NoSource.Severity(level).toString.toLowerCase

    val infoStr = s"$levelDisplay: $msg"

    val formatted = Position.formatMessage(posWithFileName, infoStr, shortenFile = true)
    formatted
  }
}

object Issue {}
