import zio._

object layers {

  trait Service1
  trait Service2
  trait Service3
  trait Service4

  val service1 = ZLayer.succeed(new Service1 {})

  val service2 = ZLayer.succeed(new Service2 {})

  val service3 = ZLayer.fromService((_: Service1) => new Service3 {})

  val service4 = ZLayer.succeed(new Service4 {})

  val services: ULayer[Has[Service1] with Has[Service2] with Has[Service3] with Has[Service4]] =
    service1 ++ service2 >+> service3 // ++ service4
}
