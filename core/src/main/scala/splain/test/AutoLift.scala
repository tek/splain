package splain.test

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.Base64
import scala.reflect.macros.blackbox

trait AutoLift {

  type Bound

  def asCode(value: Bound): String

  trait Mixin {

    val c: blackbox.Context
    import c.universe._

    implicit def _liftable[T <: Bound]: Liftable[T] = Liftable.apply { value: T =>
      val code = asCode(value)

      val parsed = c.parse(code)
      parsed
    }
  }
}

object AutoLift {

  object SerializingLift extends AutoLift {

    type Bound = Serializable

    lazy val encoder: Base64.Encoder = Base64.getEncoder
    lazy val decoder: Base64.Decoder = Base64.getDecoder

    lazy val fullPath: String = this.getClass.getCanonicalName.stripSuffix("$")

    override def asCode(value: Bound): String = {

      val bOStream = new ByteArrayOutputStream()
      val oOStream = new ObjectOutputStream(bOStream)

      oOStream.writeObject(value)

      val serialized = encoder.encodeToString(bOStream.toByteArray)

      val typeStr = value.getClass.getCanonicalName.stripSuffix("$")

      val result = s"""
         |$fullPath.fromPreviousStage[$typeStr]("$serialized")
         |""".stripMargin

      result
    }

    def fromPreviousStage[T <: Serializable](str: String): T = {

      val bytes = decoder.decode(str)

      val bIStream = new ByteArrayInputStream(bytes)
      val oIStream = new ObjectInputStream(bIStream)

      val v = oIStream.readObject()
      v.asInstanceOf[T]
    }
  }
}
