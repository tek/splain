import java.io.File

import sbt._

import Keys._

object P extends AutoPlugin {
  object autoImport {
    val versionRex = raw"(\d+)\.(\d+).(\d+).*".r

    def matchScala[A](f: PartialFunction[(Int, Int), A]): Def.Initialize[Option[A]] =
      Def.settingDyn {
        scalaVersion.value match {
          case versionRex("2", mi, pa) =>
            Def.setting(f.lift(mi.toInt, pa.toInt))
          case _ =>
            Def.setting(None)
        }
      }

    def addSourceDir(f: PartialFunction[(Int, Int), String]): Def.Setting[Seq[File]] =
      (Compile / unmanagedSourceDirectories) +=
        matchScala(f).value.map(b => sourceDirectory.value / "main" / s"scala-$b")

    def addTestSourceDir(f: PartialFunction[(Int, Int), String]): Def.Setting[Seq[File]] =
      (Test / unmanagedSourceDirectories) +=
        matchScala(f).value.map(b => sourceDirectory.value / "test" / s"scala-$b")

    val github = "https://github.com/tek"
    val repo = s"$github/splain"

    object NoAnalyzer {
      def unapply(version: (Int, Int)): Option[(Int, Int)] =
        version match {
          case (13, patch) if patch >= 6 =>
            None
          case (mi, pa) =>
            Some((mi, pa))
        }
    }
  }
}
