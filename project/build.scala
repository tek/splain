package tryp

import sbt._, Keys._

import sbtrelease.ReleasePlugin.autoImport.{ReleaseTransformations, releaseProcess, ReleaseStep}
import ReleaseTransformations._

object SplainLibs
extends Libs
{
  val splain = ids(
    scalaOrganization.value % "scala-compiler" % scalaVersion.value % "provided",
    "org.specs2" %% "specs2-core" % "3.8.6" % "test",
    "com.chuusai" %% "shapeless" % "2.3.2" % "test"
  )
}

object Build
extends MultiBuild("splain")
{
  override def defaultBuilder =
    super.defaultBuilder(_)
      .settingsV(
        scalaVersion := "2.12.4",
        crossScalaVersions ++= List("2.10.6", "2.11.11", "2.11.12", "2.12.1", "2.12.2", "2.12.3"),
        crossVersion := CrossVersion.full,
        TrypBuildKeys.libs := SplainLibs
      )

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
      } yield List(baseDirectory.value / s"src-$b")
      add.getOrElse(Nil)
    }

  lazy val splain = "splain"
    .bintray
    .settings(publishSettings)
    .settings(releaseSettings)
    .settingsV(
      organization := "io.tryp",
      name := "splain",
      addSourceDir { case (2, minor, _) if minor >= 11 => "2.11+" },
      addSourceDir { case (2, 12, patch) if patch >= 4 => "2.12.4+" case _ => "2.12.3-" },
      fork := true,
      javaOptions in Test ++= {
        val jar = (Keys.`package` in Compile).value.getAbsolutePath
        val tests = baseDirectory.value / "tests"
        List(s"-Dsplain.jar=$jar", s"-Dsplain.tests=$tests")
      }
  )

  val github = "https://github.com/tek"
  val repo = s"$github/splain"

  def publishSettings = List(
    publishMavenStyle := true,
    publishTo := Some(
      if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
      else Resolver.url("sonatype staging", url("https://oss.sonatype.org/service/local/staging/deploy/maven2"))
    ),
    licenses := List("MIT" -> url("http://opensource.org/licenses/MIT")),
    homepage := Some(url(repo)),
    scmInfo := Some(ScmInfo(url(repo), "scm:git@github.com:tek/splain")),
    developers := List(Developer(id="tryp", name="Torsten Schmits", email="torstenschmits@gmail.com", url=url(github)))
  )

  def releaseSettings = List(
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      setReleaseVersion,
      ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
      pushChanges
    )
  )
}
