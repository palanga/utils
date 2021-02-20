package palanga.aconcagua.grpc

import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import scalapb.zio_grpc.{ ServerLayer, ServiceList }
import zio.console.{ putStrLn, Console }
import zio.{ Has, ZIO }

case class GrpcApp[-R <: Has[_]](private val serviceList: ServiceList[R], private val port: Int = 9000) {

  def withPort(port: Int) = copy(port = port)

  def run: ZIO[R with Console, Throwable, Nothing] = for {
    _       <- putStrLn(s"Server is running on port $port")
    builder  = ServerBuilder.forPort(port).addService(ProtoReflectionService.newInstance())
    nothing <- ServerLayer.fromServiceList(builder, serviceList).build.useForever
  } yield nothing

}
