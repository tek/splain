package splain

import splain.PluginSettings.Keys.Key

import scala.collection.mutable
import scala.reflect.NameTransformer
import scala.util.{Failure, Success}

case class PluginSettings(pluginOpts: mutable.Map[String, String]) {}

object PluginSettings {

  case class IntKey(initV: Int) extends Keys.Key[Int] {
    override def parse(s: String): Int = s.toInt
  }

  case class BooleanKey(initV: Boolean) extends Key[Boolean] {
    override def parse(s: String): Boolean = s.toBoolean
  }

  case class StringKey(initV: String) extends Key[String] {
    override def parse(s: String): String = s
  }

  object Keys extends Enumeration {

    trait Key[T] extends Val with Product {

      def initV: T

      def parse(s: String): T

      def name: String = NameTransformer.decode(toString)
    }

    val enabled: BooleanKey = BooleanKey(true)

    val debug: BooleanKey = BooleanKey(false)

    val `Vimplicits-diverging`: BooleanKey = BooleanKey(false)

    val `Vimplicits-diverging-max-depth`: IntKey = IntKey(100)

    val `Vtype-detail`: StringKey = StringKey("1")

    val `Vtype-diffs-detail`: StringKey = StringKey("1")
  }

  lazy val nameToKey: List[(String, Key[_])] = {
    val vs = Keys.values.toList
      .collect { case v: Keys.Key[_] => v }

    vs.map { v =>
      v.name -> v
    }

  }

  lazy val nameToInitValue: List[(String, String)] = nameToKey.map { case (k, v) => k -> v.initV.toString }

  object TypeDetail extends Enumeration {

    val long: Value = Value(2)
    val existential: Value = Value(3)
    val reduction: Value = Value(4)
    val position: Value = Value(5)
    val alias: Value = Value(6)
  }

  object TypeDiffsDetail extends Enumeration {

    val disambiguation: Value = Value(2)
    val `builtin-msg`: Value = Value(3)
    val `builtin-msg-always`: Value = Value(4)
  }

  trait Implicits {

    def pluginSettings: PluginSettings

    implicit class KeyOps[T](self: Key[T]) {

      def get: T = {

        val key = self.name
        pluginSettings.pluginOpts
          .get(key)
          .map(self.parse)
          .getOrElse(
            throw new UnsupportedOperationException(s"$key is not defined")
          )
      }
    }

    implicit class BooleanKeyOps(self: BooleanKey) {

      def isEnabled: Boolean = {
        Keys.enabled.get && self.get
      }
    }

    case class DetailParsing[T <: Enumeration](key: StringKey, valueEnum: T) {

      lazy val raw: Seq[String] = key.get
        .split(',')
        .toList
        .map(_.trim)
        .filter { s =>
          s.nonEmpty
        }

      lazy val (number: Int, refined: Seq[valueEnum.Value]) = {

        val classified = raw.map { s =>
          scala.util.Try(s.toInt) match {
            case Failure(_) => Left(s)
            case Success(v) => Right(v)
          }
        }

        val refined: Seq[valueEnum.Value] = classified collect {
          case Left(v) =>
            valueEnum.withName(v)
        }
        val numbers: Seq[Int] = classified collect { case Right(v) => v }

        require(numbers.size <= 1, "only one numeric value is allowed")

        numbers.headOption.getOrElse(1) -> refined
      }

      trait ValueOps {

        val self: valueEnum.Value

        def isEnabled: Boolean = {

          Keys.enabled.get &&
          (number >= self.id || refined.contains(self))
        }
      }
    }

    val typeDetailParsing: DetailParsing[TypeDetail.type] =
      DetailParsing(Keys.`Vtype-detail`, TypeDetail)
    implicit class TypeDetailValueOps(val self: TypeDetail.Value) extends typeDetailParsing.ValueOps {}

    val typeDiffsDetailParsing: DetailParsing[TypeDiffsDetail.type] =
      DetailParsing(Keys.`Vtype-diffs-detail`, TypeDiffsDetail)
    implicit class TypeDiffsDetailValueOps(val self: TypeDiffsDetail.Value) extends typeDiffsDetailParsing.ValueOps {}

  }
}
