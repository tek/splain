val vs = versions()

buildscript {
    repositories {
        // Add here whatever repositories you're already using
        mavenCentral()
    }

//    val vs = versions()

    dependencies {
        classpath("ch.epfl.scala:gradle-bloop_2.12:1.4.9") // suffix is always 2.12, weird
    }
}

plugins {
    java
    `java-test-fixtures`

    scala

    idea

    `maven-publish`

    id("com.github.ben-manes.versions" ) version "0.39.0"
}

val rootID = vs.projectRootID

allprojects {

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "java-test-fixtures")

    // apply(plugin = "bloop")
    // DO NOT enable! In VSCode it will cause the conflict:
    // Cannot add extension with name 'bloop', as there is an extension already registered with that name

    apply(plugin = "scala")
    apply(plugin = "idea")
    apply(plugin = "maven-publish")

    group = vs.projectGroup
    version = vs.projectV

    repositories {
        mavenLocal()
        mavenCentral()
//        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
        maven("https://scala-ci.typesafe.com/artifactory/scala-integration/") // scala SNAPSHOT
    }

    dependencies {

        // see https://github.com/gradle/gradle/issues/13067
        fun bothImpl(constraintNotation: Any) {
            implementation(constraintNotation)
            testFixturesImplementation(constraintNotation)
        }

        constraints {

            bothImpl("${vs.scalaGroup}:scala-compiler:${vs.scalaV}")
            bothImpl("${vs.scalaGroup}:scala-library:${vs.scalaV}")
//            bothImpl("${vs.scalaGroup}:scala-reflect:${vs.scalaV}")
        }

//        val specs2V = "4.12.12"
//        testImplementation("org.specs2:specs2-core_${vs.scalaBinaryV}:$specs2V")// https://mvnrepository.com/artifact/org.specs2/specs2-junit
//        testImplementation("org.specs2:specs2-junit_${vs.scalaBinaryV}:$specs2V")
//        testImplementation("junit:junit:4.13.2")

        val scalaTestV = "3.2.3"
        testImplementation("org.scalatest:scalatest_${vs.scalaBinaryV}:${scalaTestV}")
        testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")

        // TODO: alpha project, switch to mature solution once https://github.com/scalatest/scalatest/issues/1454 is solved
        testRuntimeOnly("co.helmethair:scalatest-junit-runner:0.1.9")
    }

    sourceSets {
        main {
            scala {

                setSrcDirs(srcDirs + listOf("src/main/scala-2.13.6+"))
            }
        }
    }

    task("dependencyTree") {

        dependsOn("dependencies")
    }

    tasks {
        val jvmTarget = JavaVersion.VERSION_1_8.toString()

        withType<ScalaCompile> {

            targetCompatibility = jvmTarget

            scalaCompileOptions.apply {

//                    isForce = true

                loggingLevel = "verbose"

                val compilerOptions =

                    mutableListOf(

                        "-encoding", "UTF-8",
                        "-unchecked",
                        "-deprecation",
                        "-feature",

                        "-language:higherKinds",
//                            "-Xfatal-warnings",

                        "-Xlint:poly-implicit-overload",
                        "-Xlint:option-implicit",
                        "-Wunused:imports",

//                        "-Ydebug",
                        "-Yissue-debug"
//                    ,
//                    "-Ytyper-debug",
//                    "-Vtyper"

//                    ,
//                    "-Xlog-implicits",
//                    "-Xlog-implicit-conversions",
//                    "-Xlint:implicit-not-found",
//                    "-Xlint:implicit-recursion"

                    )

                additionalParameters = compilerOptions

                forkOptions.apply {

                    memoryInitialSize = "1g"
                    memoryMaximumSize = "4g"

                    // this may be over the top but the test code in macro & core frequently run implicit search on church encoded Nat type
                    jvmArgs = listOf(
                        "-Xss256m"
                    )
                }
            }
        }

        test {

            minHeapSize = "1024m"
            maxHeapSize = "4096m"

            testLogging {
                showExceptions = true
                showCauses = true
                showStackTraces = true

                // stdout is used for occasional manual verification
                showStandardStreams = true
            }

            useJUnit()
//            useJUnitPlatform {
//                includeEngines("specs2")
//                testLogging {
//                    events("passed", "skipped", "failed")
//                }
//            }
        }
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    publishing {
        val moduleID = if (project.name.startsWith(rootID)) project.name
        else rootID + "-" + project.name

        publications {
            create<MavenPublication>("maven") {
                groupId = groupId
                artifactId = moduleID
                version = version

                from(components["java"])

                suppressPomMetadataWarningsFor("testFixturesApiElements")
                suppressPomMetadataWarningsFor("testFixturesRuntimeElements")
            }
        }
    }


    idea {

        targetVersion = "2020"

        module {

            excludeDirs = excludeDirs + listOf(
                file(".gradle"),
                file(".github"),

                file ("target"),
//                        file ("out"),

                file(".idea"),
                file(".vscode"),
                file(".bloop"),
                file(".bsp"),
                file(".metals"),
                file(".ammonite"),

                file("logs"),

                file("spike"),
            )

            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }
}