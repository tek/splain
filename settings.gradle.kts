//val versions = gradle.rootProject.versions()


include(
    ":core",
    ":testing:base",
    ":testing:acceptance"
)


pluginManagement.repositories {
    gradlePluginPortal()
    mavenCentral()
    // maven("https://dl.bintray.com/kotlin/kotlin-dev")
}
