package tryp

import sbt._, Keys._

import bintray.BintrayKeys._

object SplainDeps
extends Deps
{
  val splain = ids(
    d("org.scala-lang" % "scala-compiler" % scalaVersion.value)
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
      publishMavenStyle := true
  )

  lazy val unit = "unit"
    .settingsV(
      scalacOptions in Compile ++= {
        val jar = (Keys.`package` in (splain.!, Compile)).value
        System.setProperty("sbt.paths.plugin.jar", jar.getAbsolutePath)
        val addPlugin = "-Xplugin:" + jar.getAbsolutePath
        val dummy = "-Jdummy=" + jar.lastModified
        Seq(addPlugin, dummy)
      },
      scalacOptions in Compile += "-P:splain:bounds"
    )
}
