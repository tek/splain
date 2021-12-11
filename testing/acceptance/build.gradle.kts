val vs: Versions = versions()

dependencies {

    scalaCompilerPlugins(project(":core"))

    testImplementation("${vs.scalaGroup}:scala-library:${vs.scalaV}")
    testImplementation("com.chuusai:shapeless_${vs.scalaBinaryV}:2.3.3")
//    testImplementation("dev.zio:zio_${vs.scalaBinaryV}:1.0.4")
}

tasks {

    withType<ScalaCompile> {

        scalaCompileOptions.apply {

            additionalParameters!!.addAll(
                listOf(
                    "-Vimplicits",
                    "-Vimplicits-verbose-tree",
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
