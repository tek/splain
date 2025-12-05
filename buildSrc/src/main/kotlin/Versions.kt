import org.gradle.api.Project

class Versions(private val project: Project) {

    // TODO : how to group them?
    val projectGroup = "io.tryp"
    val projectRootID = "splain"

    val projectVMajor = "1.2.0"
    val projectV = projectVMajor

    inner class Scala {
        val group: String = project.properties["scalaGroup"]?.toString() ?: "org.scala-lang"

        val v: String = project.properties["scalaVersion"].toString()
        protected val vParts: List<String> = v.split('.').also { parts ->
            require(parts.size == 3) { "Scala version must be in format 'X.Y.Z' but was: $v" }
        }

        val majorV: String = vParts[0]
        val binaryV: String = vParts.subList(0, 2).joinToString(".")
        val patchV: String = vParts[2]

        val artifactSuffix = run {
            if (majorV == "3") majorV
            else binaryV
        }

        val jsV: String? = project.properties.get("scalaJSVersion")?.toString()
    }

    val scala: Scala by lazy { Scala() }
}
