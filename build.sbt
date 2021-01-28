scalaVersion := "2.13.4"
crossScalaVersions ++= List(
  "2.13.3",
  "2.13.2",
  "2.13.1",
  "2.12.6",
  "2.12.7",
  "2.12.8",
  "2.12.9",
  "2.12.10",
  "2.12.11",
  "2.12.12",
  "2.12.13",
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

addSourceDir {
  case (2, 12, _) => "2.12"
  case (2, 13, _) => "2.13"
}
addSourceDir {
  case (2, 13, patch) if patch >= 2 => "2.13.2+"
  case (2, 12, patch) if patch >= 13 => "2.13.2+"
  case _ => "2.13.1-"
}
addTestSourceDir {
  case (2, 13, patch) if patch >= 2 => "2.13.2+"
  case _ => "2.13.1-"
}
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
