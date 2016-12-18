package tryp

import sbt._, Keys._

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
  lazy val splain = "splain".settingsV(name := "splain")

  lazy val unit = "unit"
    .settingsV(
      scalacOptions in Compile ++= {
        val jar = (Keys.`package` in (splain.!, Compile)).value
        System.setProperty("sbt.paths.plugin.jar", jar.getAbsolutePath)
        val addPlugin = "-Xplugin:" + jar.getAbsolutePath
        val dummy = "-Jdummy=" + jar.lastModified
        Seq(addPlugin, dummy)
      }
    )
}
