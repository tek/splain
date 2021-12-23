val vs: Versions = versions()

dependencies {

    scalaCompilerPlugins(project(":core"))

    testImplementation(project(":testing:base"))

//    testImplementation("com.lihaoyi:utest_${vs.scalaBinaryV}:0.7.10")
    testImplementation("com.lihaoyi:sourcecode_${vs.scalaBinaryV}:0.2.7")

    testImplementation(testFixtures(project(":core")))
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
        }
    }
}
