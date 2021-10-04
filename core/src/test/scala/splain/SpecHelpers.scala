package splain

import java.nio.file.{FileSystems, Files, Path}
import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox

object SpecHelpers {
  lazy val userDir: String = System.getProperty("user.dir").stripSuffix("/")

  lazy val types: String =
    """
      |object types
      |{
      |  class ***[A, B]
      |  class >:<[A, B]
      |  class C
      |  trait D
      |}
      |import types._
      |""".stripMargin

  lazy val base: String = {
    Option(System.getProperty("splain.tests"))
      .getOrElse(s"$userDir/src/test/run")
  }

//  def fileContent(name: String, fname: String): Path = FileSystems.getDefault.getPath(base, name, fname)
//
//  def fileContentString(name: String, fname: String): String = new String(Files.readAllBytes(fileContent(name, fname)))
//
//  def code(name: String): String = types + fileContentString(name, "code.scala")
//
//  def error(name: String, fname: Option[String]): String =
//    fileContentString(name, fname.getOrElse("error")).stripLineEnd

  val cm: universe.Mirror = universe.runtimeMirror(getClass.getClassLoader)

  val plugin: String = Option(System.getProperty("splain.jar")).getOrElse {
    val dir = FileSystems.getDefault.getPath(userDir + "/build/libs")
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

  lazy val opts: String = {
    val rows = s"""
                  |-Xplugin:$plugin
                  |-P:splain:color:false
                  |-P:splain:bounds
                  |-P:splain:tree:false
                  |""".trim.stripMargin

    rows.split('\n').mkString(" ")
  }

  def toolbox(extra: String): ToolBox[universe.type] = {
    ToolBox(cm).mkToolBox(options = s"$opts $extra")
  }
}
