val vs: Versions = versions()

dependencies {

//    testImplementation("${vs.scalaGroup}:scala-library:${vs.scalaV}")
    testFixturesApi("com.chuusai:shapeless_${vs.scalaBinaryV}:2.3.7")

    testFixturesApi("dev.zio:zio_${vs.scalaBinaryV}:1.0.4")

    testFixturesApi("org.slf4j:slf4j-api:2.0.7")
    testFixturesApi("org.slf4j:slf4j-simple:2.0.7")
}