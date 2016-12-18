resolvers += Resolver.url(
  "bintray-tek-sbt",
  url("https://dl.bintray.com/tek/sbt-plugins")
)(Resolver.ivyStylePatterns)
libraryDependencies += Defaults.sbtPluginExtra(
  "tryp.sbt" % "tryp-build" % trypVersion.value,
  (sbtBinaryVersion in update).value,
  (scalaBinaryVersion in update).value
)
