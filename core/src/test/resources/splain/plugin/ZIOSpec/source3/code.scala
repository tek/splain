import zio.*
import zio.console.Console

object source3 {

  object Server {
    type Pizza
    type Broccoli

    val start: ZIO[Console & Broccoli, Nothing, Unit] = {
      val handler = (ServiceImpl.foo _).tupled
      handler("blah", 1).unit
    }
  }

  import Server.*

  trait Service[-R] {
    def foo(s: String, i: Int): ZIO[R, Nothing, String]
  }

  object ServiceImpl extends Service[Console & Pizza] {
    def foo(s: String, i: Int) =
      zio.console.putStrLn("blah").orDie.as("")
  }
}
