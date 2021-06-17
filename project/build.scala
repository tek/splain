import java.io.File

import sbt._

import Keys._

object P extends AutoPlugin {
  object autoImport {
    val versionRex = raw"(\d+)\.(\d+).(\d+).*".r

    def matchScala[A](f: PartialFunction[(Int, Int, Int), A]): Def.Initialize[Option[A]] =
      Def.settingDyn {
        scalaVersion.value match {
          case versionRex(ma, mi, pa) =>
            Def.setting(f.lift(ma.toInt, mi.toInt, pa.toInt))
          case _ =>
            Def.setting(None)
        }
      }

    def addSourceDir(f: PartialFunction[(Int, Int, Int), String]): Def.Setting[Seq[File]] =
      (unmanagedSourceDirectories in Compile) +=
        matchScala(f).value.map(b => sourceDirectory.value / "main" / s"scala-$b")

    def addTestSourceDir(f: PartialFunction[(Int, Int, Int), String]): Def.Setting[Seq[File]] =
      (unmanagedSourceDirectories in Test) +=
        matchScala(f).value.map(b => sourceDirectory.value / "test" / s"scala-$b")

    val github = "https://github.com/tek"
    val repo = s"$github/splain"
  }
}
