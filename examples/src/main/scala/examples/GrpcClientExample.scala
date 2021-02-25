package examples

import examples.painter.ZioPainter.PaintersClient
import examples.painter.Empty
import io.grpc.ManagedChannelBuilder
import zio.{ ExitCode, URIO }

// TODO esto es una verga
object GrpcClientExample extends zio.App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    PaintersClient
      .painters(Empty())
      .runCollect
      .provideCustomLayer(
        PaintersClient.live(
          scalapb.zio_grpc.ZManagedChannel(
            ManagedChannelBuilder.forAddress("localhost", 9000).usePlaintext()
          )
        )
      )
      .exitCode

}
