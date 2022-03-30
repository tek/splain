package splain.test

import scala.collection.mutable.ArrayBuffer
import scala.tools.reflect.FrontEnd

// mimic of StoreReporter
case class CachingFrontEnd(sourceName: String) extends FrontEnd {

  val cached: ArrayBuffer[Issue] = ArrayBuffer.empty[Issue]

  override def display(info: Info): Unit = {

    val issue = Issue(
      info.severity.id,
      info.msg,
      info.pos,
      sourceName
    )

    this.cached += issue
  }

  override def reset(): Unit = {
    super.reset()
    cached.clear()
  }
}

object CachingFrontEnd {

  object NoSource extends CachingFrontEnd("")
}
