import org.gradle.util.internal.VersionNumber

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.specs.Spec

val vs = versions()

buildscript {
    repositories {
        // Add here whatever repositories you're already using
        mavenCentral()
    }

    dependencies {
        classpath("ch.epfl.scala:gradle-bloop_2.12:1.6.4") // suffix is always 2.12, weird
    }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    filterConfigurations = Spec<Configuration> {
        !it.name.startsWith("incrementalScalaAnalysis")
    }
}

plugins {
    `java-test-fixtures`

    scala

    idea

    signing
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"

    id("com.github.ben-manes.versions") version "0.53.0"

    id("io.github.cosmicsilence.scalafix") version "0.2.6"
}

val sonatypeApiUser = providers.gradleProperty("sonatypeApiUser")
val sonatypeApiKey = providers.gradleProperty("sonatypeApiKey")
if (sonatypeApiUser.isPresent && sonatypeApiKey.isPresent) {
    nexusPublishing {
        repositories {
            sonatype {

                nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
                snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))

                username.set(sonatypeApiUser)
                password.set(sonatypeApiKey)
//                useStaging.set(true)
            }
        }
    }
} else {
    logger.warn("Sonatype API key not defined, skipping configuration of Maven Central publishing repository")
}

allprojects {

    apply(plugin = "java-library")
    apply(plugin = "java-test-fixtures")

    // apply(plugin = "bloop")
    // DO NOT enable! In VSCode it will cause the conflict:
    // Cannot add extension with name 'bloop', as there is an extension already registered with that name

    apply(plugin = "scala")
    apply(plugin = "idea")

    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    group = vs.projectGroup
    version = vs.projectV

    repositories {
        mavenCentral()
//        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
        maven("https://scala-ci.typesafe.com/artifactory/scala-integration/") // scala SNAPSHOT
    }

    fun includeShims(from: String, to: String) {
        sourceSets {
            main {
                scala {
                    setSrcDirs(srcDirs + listOf("src/main/scala-${from}+/${to}"))
                }
                resources {
                    setSrcDirs(srcDirs + listOf("src/main/resources-${from}+/${to}"))
                }
            }
            testFixtures {
                scala {
                    setSrcDirs(srcDirs + listOf("src/testFixtures/scala-${from}+/${to}"))
                }
                resources {
                    setSrcDirs(srcDirs + listOf("src/testFixtures/resources-${from}+/${to}"))
                }
            }
            test {
                scala {
                    setSrcDirs(srcDirs + listOf("src/test/scala-${from}+/${to}"))
                }
                resources {
                    setSrcDirs(srcDirs + listOf("src/test/resources-${from}+/${to}"))
                }
            }
        }
    }

    val vn = VersionNumber.parse(vs.scala.v)
    val supportedPatchVs = 7..12

    for (from in supportedPatchVs) {
        if (vn.micro >= from) {

            includeShims("2.13.${from}", "latest")
        }
        for (to in supportedPatchVs) {
            if (vn.micro <= to) {

                includeShims("2.13.${from}", "2.13.${to}")
            }
        }
    }

    dependencies {

        constraints {}

        implementation("${vs.scala.group}:scala-library:${vs.scala.v}")

        val scalaTestV = "3.2.11"
        testFixturesApi("org.scalatest:scalatest_${vs.scala.artifactSuffix}:${scalaTestV}")
        testImplementation("org.scalatest:scalatest_${vs.scala.artifactSuffix}:${scalaTestV}")
//        testFixturesApi("org.scalatest:scalatest-core_${vs.scala.artifactSuffix}:${vs.scalaTestV}")

        val jUnitV = "5.13.4"
        val jUnitPlatformV = "1.13.4"

        testRuntimeOnly("org.junit.platform:junit-platform-engine:$jUnitPlatformV")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher:$jUnitPlatformV")
        testImplementation("org.junit.jupiter:junit-jupiter:${jUnitV}")
//        testRuntimeOnly("org.scalatestplus:junit-5-13_${vs.scala.artifactSuffix}:3.2.19.0")
        testRuntimeOnly("ai.acyclic.scalatestplus:junit-5-13_${vs.scala.artifactSuffix}:3.2.19.2")
//        testRuntimeOnly("ai.acyclic.scalatestplus:junit-5-13_${vs.scala.artifactSuffix}:3.3.0.0")
    }

    tasks.register("dependencyTree") {

        dependsOn("dependencies")
    }

    val jvmTarget = JavaVersion.VERSION_1_8

    java {

        withSourcesJar()
        withJavadocJar()

        sourceCompatibility = jvmTarget
        targetCompatibility = jvmTarget
    }

    tasks {

        withType<ScalaCompile> {

            sourceCompatibility = jvmTarget.toString()
            targetCompatibility = jvmTarget.toString()

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
                        "-Ywarn-unused:patvars"
//                        "-Ydebug-error"
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

    apply(plugin = "io.github.cosmicsilence.scalafix")
    scalafix {
        semanticdb.autoConfigure.set(true)
        semanticdb.version.set("4.9.0")
    }

    idea {

        module {

            excludeDirs = excludeDirs + files(

                "target",
                "out",

                ".idea",
                ".vscode",
                ".bloop",
                ".bsp",
                ".metals",
                "bin",

                ".ammonite",

                "logs",

                )

            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }
}

subprojects {

    // https://stackoverflow.com/a/66352905/1772342

//    val signingKeyID = providers.gradleProperty("signing.gnupg.keyID")
    val signingSecretKey = providers.gradleProperty("signing.gnupg.secretKey")
    val signingKeyPassphrase = providers.gradleProperty("signing.gnupg.passphrase")
    signing {
        useGpgCmd()
        if (signingSecretKey.isPresent) {
            useInMemoryPgpKeys(signingSecretKey.get(), signingKeyPassphrase.get())
//            useInMemoryPgpKeys(signingKeyID.get(), signingSecretKey.get(), signingKeyPassphrase.get())
            sign(extensions.getByType<PublishingExtension>().publications)
        } else {
            logger.warn("PGP signing key not defined, skipping signing configuration")
        }
    }

    publishing {
        val suffix = "_" + vs.scala.v

        val rootID = vs.projectRootID

        val moduleID =
            if (project.name.equals(rootID))// rootID + "-" + "parent" + suffix
                throw kotlin.UnsupportedOperationException("root project should not be published")
            else if (project.name.equals("core")) rootID + suffix
            else rootID + "-" + project.name + suffix

        val whitelist = setOf("core")

        if (whitelist.contains(project.name)) {

            publications {
                create<MavenPublication>("maven") {

                    val javaComponent = components["java"] as AdhocComponentWithVariants
                    from(javaComponent)

                    javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
                    javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

                    artifactId = moduleID
                    version = project.version.toString()

                    pom {
                        licenses {
                            license {
                                name.set("MIT")
                                url.set("http://opensource.org/licenses/MIT")
                            }
                        }

                        name.set("splain")
                        description.set("A scala compiler plugin for more concise errors")

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
                }
            }
        }
    }
}

idea {

    targetVersion = "2020"

    module {

        excludeDirs = excludeDirs + files(
            ".gradle",
            "gradle",
            "spike",
            ".history"
        )
    }
}