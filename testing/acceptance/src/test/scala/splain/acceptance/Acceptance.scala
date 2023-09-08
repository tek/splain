package splain.acceptance

import org.scalatest.funspec.AnyFunSpec
import splain.TestHelpers
import splain.test.TryCompile

import scala.collection.mutable.ArrayBuffer

object Acceptance {

  final val static = TryCompile.Static()

  trait SpecBase extends AnyFunSpec with TestHelpers with static.FromCodeMixin {

    lazy val buffer: ArrayBuffer[TryCompile] = ArrayBuffer.empty

    def check(v: TryCompile): Unit = {
      buffer += v
    }

    it("acceptance") {

      val issues = buffer.flatMap(v => v.issues).mkString("\n")

      val runner = DirectRunner()

      val gt = runner.DefaultGroundTruths.raw

      issues must_== gt
    }
  }
}
