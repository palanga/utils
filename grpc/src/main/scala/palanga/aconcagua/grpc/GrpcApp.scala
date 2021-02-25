package palanga.aconcagua.grpc

import io.grpc._
import io.grpc.protobuf.services.ProtoReflectionService
import palanga.aconcagua
import palanga.aconcagua.grpc.GrpcApp.withMetrics
import scalapb.zio_grpc.{ ServerLayer, ServiceList }
import zio.console.{ putStrLn, Console }
import zio.zmx.metrics.ZMX
import zio.{ Has, ZIO }

case class GrpcApp[-R <: Has[_]](
  private val services: ServiceList[R],
  private val port: Int = 9000,
  private val defaultMetrics: Boolean = false,
) extends aconcagua.App[R] {

  override def withPort(port: Int): aconcagua.App[R] = copy(port = port)
  override def withDefaultMetrics: aconcagua.App[R]  = copy(defaultMetrics = true)

  def run: ZIO[R with Console, Throwable, Nothing] =
    putStrLn(s"Server is running on port $port") &>
      ServerLayer
        .fromServiceList(serverBuilder, services)
        .build
        .useForever

  private def serverBuilder = {
    val res =
      ServerBuilder
        .forPort(port)
        .addService(ProtoReflectionService.newInstance())
    if (defaultMetrics) res.intercept(withMetrics)
    else res
  }

}

object GrpcApp {
  private def withMetrics: ServerInterceptor =
    new ServerInterceptor() {
      override def interceptCall[Req, Res](
        call: ServerCall[Req, Res],
        headers: Metadata,
        next: ServerCallHandler[Req, Res],
      ): ServerCall.Listener[Req] = {
        // TODO is there a better way to do this ?
        zio.Runtime.default.unsafeRun(ZMX.count(aconcagua.metrics.TOTAL_REQUESTS, 1d))
        next.startCall(call, headers)
      }
    }
}
