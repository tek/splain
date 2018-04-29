scalaVersion := "2.12.6"
crossScalaVersions ++= List("2.11.12", "2.12.1", "2.12.2", "2.12.3", "2.12.4", "2.12.5")
crossVersion := CrossVersion.full
organization := "io.tryp"
name := "splain"
fork := true
libraryDependencies ++= List(
  scalaOrganization.value % "scala-compiler" % scalaVersion.value % "provided",
  "org.specs2" %% "specs2-core" % "4.1.0" % "test",
  "com.chuusai" %% "shapeless" % "2.3.3" % "test",
)

addSourceDir { case (2, minor, _) if minor >= 11 => "2.11+" }
addSourceDir { case (2, 12, patch) if patch >= 4 => "2.12.4+" case _ => "2.12.3-" }
javaOptions in Test ++= {
  val jar = (Keys.`package` in Compile).value.getAbsolutePath
    val tests = baseDirectory.value / "tests"
    List(s"-Dsplain.jar=$jar", s"-Dsplain.tests=$tests")
}

publishMavenStyle := true
publishTo := Some(
  if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
  else Resolver.url("sonatype staging", url("https://oss.sonatype.org/service/local/staging/deploy/maven2"))
)
licenses := List("MIT" -> url("http://opensource.org/licenses/MIT"))
homepage := Some(url(repo))
scmInfo := Some(ScmInfo(url(repo), "scm:git@github.com:tek/splain"))
developers := List(Developer(id="tryp", name="Torsten Schmits", email="torstenschmits@gmail.com", url=url(github)))

import ReleaseTransformations._
releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeReleaseAll"),
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion
)
