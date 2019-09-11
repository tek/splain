scalaVersion := "2.13.0"
crossScalaVersions ++= List(
  "2.10.7",
  "2.11.12",
  "2.12.1",
  "2.12.2",
  "2.12.3",
  "2.12.4",
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

libraryDependencies += {
  val specsV =
    matchScala { case (ma, mi, _) if ma == 2 && mi == 10 => "3.9.5" }
      .value
      .getOrElse("4.5.1")
  "org.specs2" %% "specs2-core" % specsV % "test"
}

addSourceDir { case (2, minor, _) if minor >= 11 => "2.11+" }
addSourceDir { case (2, minor, _) if minor <= 11 => "2.11-" }
addSourceDir { case (2, minor, _) if minor == 12 => "2.1-" }
addSourceDir { case (2, 12, patch) if patch >= 4 => "2.12.4+" case (2, minor, _) if minor <= 12 => "2.12.3-" }
addSourceDir { case (2, minor, _) if minor >= 13 => "2.13+" }
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
