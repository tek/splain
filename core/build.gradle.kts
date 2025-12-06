val vs: Versions = versions()

dependencies {

    // see https://github.com/gradle/gradle/issues/13067
    fun bothImpl(constraintNotation: Any) {
        implementation(constraintNotation)
        testFixturesImplementation(constraintNotation)
    }

    bothImpl("${vs.scala.group}:scala-compiler:${vs.scala.v}")

    testFixturesApi("com.chuusai:shapeless_${vs.scala.binaryV}:2.3.7")

    testFixturesApi("dev.zio:zio_${vs.scala.binaryV}:1.0.18")

    testFixturesApi("org.slf4j:slf4j-api:2.0.9")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
}