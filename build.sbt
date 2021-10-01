scalaVersion := "2.13.6"
crossScalaVersions ++= List(
  "2.13.6"
)
crossVersion := CrossVersion.full
organization := "io.tryp"
name := "splain"
fork := true
libraryDependencies ++= List(
  scalaOrganization.value % "scala-compiler" % scalaVersion.value % "provided",
  "com.chuusai" %% "shapeless" % "2.3.3" % "test",
  "dev.zio" %% "zio" % "1.0.4" % "test"
)

libraryDependencies += "org.specs2" %% "specs2-core" % "4.12.12" % Test

addSourceDir {
  case (12, _) =>
    "2.12"
  case NoAnalyzer((13, patch)) if patch < 6 =>
    "2.13-"
}
addSourceDir {
  case NoAnalyzer((13, patch)) if patch >= 2 =>
    "2.13.2+"
  case (12, patch) if patch >= 13 =>
    "2.13.2+"
  case NoAnalyzer((_, _)) =>
    "2.13.1-"
}
addSourceDir {
  case (12, patch) if patch >= 14 =>
    "2.12.14+"
  case (12, _) =>
    "2.12.13-"
  case NoAnalyzer((13, _)) =>
    "2.12.13-"
}
addSourceDir {
  case (13, patch) if patch >= 6 =>
    "2.13.6+"
  case _ =>
    "2.13.5-"
}
addTestSourceDir {
  case NoAnalyzer((13, patch)) if patch >= 2 =>
    "2.13.2+"
  case NoAnalyzer((_, _)) =>
    "2.13.1-"
  case _ =>
    "2.13.6+"
}
Test / javaOptions ++= {
  val jar = (Compile / Keys.`package`).value.getAbsolutePath
  val tests = baseDirectory.value / "src/test/run"
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
  "-Ywarn-unused:patvars"
)

publishMavenStyle := true
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Resolver.url("sonatype staging", url("https://oss.sonatype.org/service/local/staging/deploy/maven2"))
)
licenses := List("MIT" -> url("http://opensource.org/licenses/MIT"))
homepage := Some(url(repo))
scmInfo := Some(ScmInfo(url(repo), "scm:git@github.com:tek/splain"))
developers := List(
  Developer(id = "tryp", name = "Torsten Schmits", email = "torstenschmits@gmail.com", url = url(github))
)

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
