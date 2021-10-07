package splain

import org.scalatest.funspec.AnyFunSpec

import scala.util.Try

trait SpecBase extends AnyFunSpec with SpecFeatures {

  protected def _it: ItWord = it
}

object SpecBase {

  trait Direct extends SpecBase {

    // will use reflection to discover all type `() => String` method under this instance
    lazy val codeToName: Map[String, String] = {

      val methods = this.getClass.getDeclaredMethods.filter { method =>
        method.getParameterCount == 0 &&
        method.getReturnType == classOf[String]
//        method.isAccessible
      }

      val seq = methods.flatMap { method =>
        Try {
          val code = method.invoke(this).asInstanceOf[String]
          code -> method.getName
        }.toOption
      }
      Map(seq: _*)
    }

    lazy val runner = DirectRunner()

    def check(code: String): Unit = {

      val name = codeToName(code)

      _it(name) {
        runner.run(code)
      }
    }
  }

  trait File extends SpecBase {

    def check(
        name: String,
        file: String = "",
        extra: String = extraSettings
    )(
        check: CheckFile
    ): Unit = {

      val _file =
        if (file.isEmpty) name
        else file

      _it(name) {

        check(FileCase(_file, extra))
      }
    }
  }
}
