package splain.test

import scala.collection.immutable.ArraySeq
import scala.reflect.internal.util.{BatchSourceFile, NoPosition, Position}
import scala.util.Try

@SerialVersionUID(5124466488126506935L)
case class Issue(
    severity: Int,
    msg: String,
    @transient pos: Position,
    sourceName: String = Issue.defaultSrcName,
    isShortName: Boolean = false
) extends Serializable {

  // mimic of PrintReporter.display
  val display: String = {

    lazy val severityDisplay: String = new CachingFrontEnd.NoSource.Severity(severity).toString.toLowerCase

    val posWithFileName = Try(
      pos.withSource(
        new BatchSourceFile(sourceName, ArraySeq.unsafeWrapArray(pos.source.content))
      )
    )

    val infoStr = s"$severityDisplay: $msg"

    val result = posWithFileName
      .map { pos =>
        val formatted = Position.formatMessage(pos, infoStr, shortenFile = true)
        formatted
      }
      .recover {
        case ee: Exception =>
          val formatted = Position.formatMessage(
            NoPosition,
            s"""$infoStr
               |with error: $ee
               |""".stripMargin,
            shortenFile = true
          )
          formatted

      }
      .get

    result
  }

  override def toString: String = {

    display
  }
}

object Issue {

  lazy val defaultSrcName: "newSource1.scala" = "newSource1.scala"
}
