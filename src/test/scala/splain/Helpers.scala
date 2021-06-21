package splain

import java.nio.file.{FileSystems, Files, Path}

import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox

object Helpers {
  lazy val userDir = System.getProperty("user.dir").stripSuffix("/")

  def base =
    Option(System.getProperty("splain.tests"))
      .getOrElse(userDir + "/" + "tests")

  def fileContent(name: String, fname: String): Path = FileSystems.getDefault.getPath(base, name, fname)

  def fileContentString(name: String, fname: String): String = new String(Files.readAllBytes(fileContent(name, fname)))

  def types = """object types
{
  class ***[A, B]
  class >:<[A, B]
  class C
  trait D
}
import types._
"""

  def code(name: String) = types + fileContentString(name, "code.scala")

  def error(name: String, fname: Option[String]) = fileContentString(name, fname.getOrElse("error")).stripLineEnd

  val cm = universe.runtimeMirror(getClass.getClassLoader)

  val plugin = Option(System.getProperty("splain.jar")).getOrElse {
    val dir = FileSystems.getDefault.getPath(userDir + "/target/scala-2.13")
    val file =
      Files
        .list(dir)
        .toArray
        .map(v => v.asInstanceOf[Path])
        .filter(v => v.toString.endsWith(".jar"))
        .filterNot { v =>
          v.toString.endsWith("-javadoc.jar") ||
          v.toString.endsWith("-sources.jar")
        }
        .head

    file.toAbsolutePath.toString
  }

  val opts = s"-Xplugin:$plugin -P:splain:color:false -P:splain:bounds -P:splain:tree:false"

  def toolbox(extra: String) = ToolBox(cm).mkToolBox(options = s"$opts $extra")
}
