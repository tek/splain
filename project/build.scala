package tryp

import sbt._, Keys._

import sbtrelease.ReleasePlugin.autoImport.{ReleaseTransformations, releaseProcess, ReleaseStep}
import ReleaseTransformations._

object SplainDeps
extends Deps
{
  val splain = ids(
    d(scalaOrganization.value % "scala-compiler" % scalaVersion.value % "provided"),
    "org.specs2" %% "specs2-core" % "3.8.6" % "test",
    "com.chuusai" %% "shapeless" % "2.3.2" % "test"
  )
}

object Build
extends MultiBuild("splain", deps = SplainDeps)
{
  override def defaultBuilder =
    super.defaultBuilder(_)
      .settingsV(
        scalaVersion := "2.12.3",
        crossScalaVersions ++= List("2.10.6", "2.11.11")
      )

  lazy val splain = "splain"
    .bintray
    .settings(publishSettings)
    .settings(releaseSettings)
    .settingsV(
      organization := "io.tryp",
      name := "splain",
      fork := true,
      javaOptions in Test ++= {
        val jar = (Keys.`package` in Compile).value.getAbsolutePath
        val tests = baseDirectory.value / "tests"
        List(s"-Dsplain.jar=$jar", s"-Dsplain.tests=$tests")
      },
      (unmanagedSourceDirectories in Compile) ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, y)) if y >= 11 =>
            List(baseDirectory.value / "src-2.11+")
          case _ => List()
        }
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
