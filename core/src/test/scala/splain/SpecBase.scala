package splain

import org.scalatest.funspec.AnyFunSpec

import scala.util.Try

trait SpecBase extends AnyFunSpec with TestHelpers {

  protected def _it: ItWord = it
}

object SpecBase {

  trait Direct extends SpecBase {

    // will use reflection to discover all type `() => String` method under this instance
    lazy val codeToName: Map[String, String] = {

      val methods = this.getClass.getDeclaredMethods.filter { method =>
        method.getParameterCount == 0 &&
        method.getReturnType == classOf[String]
      }

      val seq = methods.flatMap { method =>
        Try {
          val code = method.invoke(this).asInstanceOf[String]
          code -> method.getName
        }.toOption
      }.toSeq
      Map(seq: _*)
    }

    def getName(code: String, nameOverride: String): String = {
      if (nameOverride.nonEmpty) nameOverride
      else codeToName(code)
    }

    lazy val runner: DirectRunner = DirectRunner()

    def check(code: String, extra: String = defaultExtra, nameOverride: String = "", numberOfBlocks: Int = 1): Unit = {

      val name = getName(code, nameOverride)
      val cc = DirectCase(code, extra)

      val from = runner.pointer.getAndAdd(numberOfBlocks)
      val until = runner.pointer.get()
      val groundTruth = runner.groundTruths.slice(from, until).mkString("\n")

      _it(name) {
        val error = cc.splainC.compileError()
        error must_== groundTruth
      }
    }

    def skip(code: String, extra: String = defaultExtra, nameOverride: String = "", numberOfBlocks: Int = 1): Unit = {

      val name = getName(code, nameOverride)

      runner.pointer.getAndAdd(numberOfBlocks)

      ignore(name) {}
    }
  }

  trait File extends SpecBase {

    def check(
        name: String,
        file: String = "",
        extra: String = defaultExtra
    )(
        check: CheckFile
    ): Unit = {

      val _file =
        if (file.isEmpty) name
        else file

      val testName = Seq(name, file).filter(_.nonEmpty).mkString(" - ")

      _it(testName) {

        check(FileCase(_file, extra))
      }
    }

    def skip(
        name: String,
        file: String = "",
        extra: String = defaultExtra
    )(
        check: CheckFile
    ): Unit = {

      val _file =
        if (file.isEmpty) name
        else file

      val testName = Seq(name, file).filter(_.nonEmpty).mkString(" - ")

      ignore(testName) {

        check(FileCase(_file, extra))
      }
    }
  }
}
