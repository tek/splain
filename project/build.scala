package tryp

import sbt._, Keys._

object P
extends AutoPlugin
{
  object autoImport
  {
    val versionRex = raw"(\d+)\.(\d+).(\d+).*".r

    def addSourceDir(f: PartialFunction[(Int, Int, Int), String]) =
      (unmanagedSourceDirectories in Compile) ++= {
        val add = for {
          a <- scalaVersion.value match {
            case versionRex(ma, mi, pa) => Some(ma.toInt, mi.toInt, pa.toInt)
            case _ => None
          }
          (ma, mi, pa) = a
          b <- f.lift(ma, mi, pa)
        } yield List(sourceDirectory.value / "main" / s"scala-$b")
        add.getOrElse(Nil)
      }

    val github = "https://github.com/tek"
    val repo = s"$github/splain"
  }
}
