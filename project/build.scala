package tryp

import sbt._, Keys._

import bintray.BintrayKeys._

object SplainDeps
extends Deps
{
  val splain = ids(
    d("org.scala-lang" % "scala-compiler" % scalaVersion.value),
    "org.specs2" %% "specs2-core" % "3.8.6" % "test",
    "com.chuusai" %% "shapeless" % "2.3.2" % "test"
  )
}

object Build
extends MultiBuild("splain", deps = SplainDeps)
{
  override def defaultBuilder =
    super.defaultBuilder(_)
      .settingsV(crossScalaVersions += "2.12.1")

  lazy val splain = "splain"
    .bintray
    .settingsV(
      name := "splain",
      licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
      bintrayRepository in bintray := "releases",
      publishMavenStyle := true,
      fork := true,
      javaOptions in Test ++= {
        val jar = (Keys.`package` in Compile).value.getAbsolutePath
        val tests = baseDirectory.value / "tests"
        List(s"-Dsplain.jar=$jar", s"-Dsplain.tests=$tests")
      }
  )
}
