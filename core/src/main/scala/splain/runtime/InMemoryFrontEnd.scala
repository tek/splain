package splain.runtime

import scala.collection.immutable.ArraySeq
import scala.reflect.internal.util.{BatchSourceFile, Position}
import scala.tools.reflect.FrontEnd

case class InMemoryFrontEnd(sourceName: String) extends FrontEnd {

  @volatile var msg: String = _

  override def display(info: Info): Unit = {

    val posWithFileName = info.pos.withSource(
      new BatchSourceFile(sourceName, ArraySeq.unsafeWrapArray(info.pos.source.content))
    )

    val infoStr = s"${info.severity.toString().toLowerCase}: ${info.msg}"

    val msg = Position.formatMessage(posWithFileName, infoStr, shortenFile = true)
    this.msg = msg
  }
}
