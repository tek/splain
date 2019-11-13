scalaVersion := "2.13.1"
crossScalaVersions ++= List(
  "2.12.5",
  "2.12.6",
  "2.12.7",
  "2.12.8",
  "2.12.9",
  "2.12.10",
)
crossVersion := CrossVersion.full
organization := "io.tryp"
name := "splain"
fork := true
libraryDependencies ++= List(
  scalaOrganization.value % "scala-compiler" % scalaVersion.value % "provided",
  "com.chuusai" %% "shapeless" % "2.3.3" % "test",
)

libraryDependencies += "org.specs2" %% "specs2-core" % "4.5.1" % Test

addSourceDir { case (2, minor, _) if minor == 12 => "2.12" }
addSourceDir { case (2, minor, _) if minor == 13 => "2.13" }
javaOptions in Test ++= {
  val jar = (Keys.`package` in Compile).value.getAbsolutePath
    val tests = baseDirectory.value / "tests"
    List(s"-Dsplain.jar=$jar", s"-Dsplain.tests=$tests")
}

scalacOptions ++= List(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:higherKinds",
  "-language:existentials",
  "-Ywarn-value-discard",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:implicits",
  "-Ywarn-unused:params",
  "-Ywarn-unused:patvars",
)

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
  releaseStepCommandAndRemaining("+publish"),
  releaseStepCommand("sonatypeReleaseAll"),
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion
)
