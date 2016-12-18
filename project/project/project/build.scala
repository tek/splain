import sbt._

object P
extends Plugin
{
  val trypVersion = settingKey[String]("tryp version")
}