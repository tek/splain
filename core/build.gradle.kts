val vs: Versions = versions()

dependencies {

    // see https://github.com/gradle/gradle/issues/13067
    fun bothImpl(constraintNotation: Any) {
        implementation(constraintNotation)
        testFixturesImplementation(constraintNotation)
    }

    bothImpl("${vs.scalaGroup}:scala-compiler:${vs.scalaV}")

//    testImplementation("${vs.scalaGroup}:scala-library:${vs.scalaV}")
    testFixturesApi("com.chuusai:shapeless_${vs.scalaBinaryV}:2.3.7")

    testFixturesApi("dev.zio:zio_${vs.scalaBinaryV}:1.0.18")

    testFixturesApi("org.slf4j:slf4j-api:2.0.9")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
}