val vs: Versions = versions()

dependencies {

    testImplementation("${vs.scalaGroup}:scala-library:${vs.scalaV}")
    testImplementation("com.chuusai:shapeless_${vs.scalaBinaryV}:2.3.3")
    testImplementation("dev.zio:zio_${vs.scalaBinaryV}:1.0.4")
}