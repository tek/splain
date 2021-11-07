val vs = versions()

buildscript {
    repositories {
        // Add here whatever repositories you're already using
        mavenCentral()
    }

    dependencies {
        classpath("ch.epfl.scala:gradle-bloop_2.12:1.4.11") // suffix is always 2.12, weird
    }
}

plugins {
    java
    `java-test-fixtures`

    scala

    idea

    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"

    id("com.github.ben-manes.versions" ) version "0.39.0"
}

val rootID = vs.projectRootID

val sonatypeApiUser = providers.gradleProperty("sonatypeApiUser")
val sonatypeApiKey = providers.gradleProperty("sonatypeApiKey")
if (sonatypeApiUser.isPresent && sonatypeApiKey.isPresent) {
    nexusPublishing {
        repositories {
            sonatype {

//                nexusUrl.set(uri("https://oss.sonatype.org/service/local/"))
//                snapshotRepositoryUrl.set(uri("https://oss.sonatype.org/content/repositories/snapshots/"))

                username.set(sonatypeApiUser)
                password.set(sonatypeApiKey)
                useStaging.set(true)
            }
        }

// TODO: migrate this sbt process
//import ReleaseTransformations._
//        releaseCrossBuild := true
//releaseProcess := Seq[ReleaseStep](
//    checkSnapshotDependencies,
//    inquireVersions,
//    runClean,
//    setReleaseVersion,
//    releaseStepCommandAndRemaining("+publish"),
//    releaseStepCommand("sonatypeReleaseAll"),
//    commitReleaseVersion,
//    tagRelease,
//    setNextVersion,
//    commitNextVersion,
//)
    }
} else {
    logger.info("Sonatype API key not defined, skipping configuration of Maven Central publishing repository")
}

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
        fun both(constraintNotation: Any) {
            implementation(constraintNotation)
            testFixturesImplementation(constraintNotation)
        }

        constraints {

            both("${vs.scalaGroup}:scala-compiler:${vs.scalaV}")
            both("${vs.scalaGroup}:scala-library:${vs.scalaV}")
        }

        val scalaTestV = "3.2.3"
        testImplementation("org.scalatest:scalatest_${vs.scalaBinaryV}:${scalaTestV}")
        testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")

        testRuntimeOnly("co.helmethair:scalatest-junit-runner:0.1.10")
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

                        "-deprecation",
                        "-unchecked",
                        "-feature",
                        "-language:higherKinds",
                        "-language:existentials",
                        "-Ywarn-value-discard",
                        "-Ywarn-unused:imports",
                        "-Ywarn-unused:implicits",
                        "-Ywarn-unused:params",
                        "-Ywarn-unused:patvars",
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

//            useJUnit()
            useJUnitPlatform {
                includeEngines("scalatest")
                testLogging {
                    events("passed", "skipped", "failed")
                }
            }
        }
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }
}

subprojects {

    publishing {
        val suffix = "_" + vs.scalaV

        val moduleID =
            if (project.name.equals(rootID)) rootID + "-" + "parent" + suffix
            else if (project.name.equals("core")) rootID + suffix
            else rootID + "-" + project.name + suffix

        publications {

            create<MavenPublication>("maven") {

                from(components["java"])

                groupId = groupId
                artifactId = moduleID
                version = version

                pom {
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("http://opensource.org/licenses/MIT\"")
                        }
                    }

                    val github = "https://github.com/tek"
                    val repo = github + "/splain"

                    url.set(repo)

                    developers {
                        developer {
                            id.set("tryp")
                            name.set("Torsten Schmits")
                            email.set("torstenschmits@gmail.com")
                            url.set(github)
                        }
                    }
                    scm {
                        connection.set("scm:git@github.com:tek/splain")
                        url.set(repo)
                    }
                }

                suppressPomMetadataWarningsFor("testFixturesApiElements")
                suppressPomMetadataWarningsFor("testFixturesRuntimeElements")
            }
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
