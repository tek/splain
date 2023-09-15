val vs: Versions = versions()

dependencies {

    testImplementation(project(":core"))
    testFixturesApi(testFixtures(project(":core")))

    scalaCompilerPlugins(project(":core"))

//    testImplementation("com.chuusai:shapeless_${vs.scalaBinaryV}:2.3.7")
//    testImplementation("dev.zio:zio_${vs.scalaBinaryV}:1.0.4")
}

tasks {

    var minJavaV = JavaVersion.VERSION_1_8.toString()

    withType<ScalaCompile> {

        targetCompatibility = minJavaV

        scalaCompileOptions.apply {

            additionalParameters!!.addAll(
                listOf(
                    "-Vimplicits",
                    "-Vtype-diffs",
                    "-P:splain:Vimplicits-diverging"
                )
            )

//            println("===== SCALAC ARGS ======")
//            additionalParameters!!.forEach {
//                v ->
//                println(v)
//            }
        }
    }
}
