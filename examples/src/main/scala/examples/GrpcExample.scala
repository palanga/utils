package examples

import examples.painter.{ Empty, Museum, Painter, PainterRequest }
import examples.painter.ZioPainter.{ ZMuseums, ZPainters }
import io.grpc.Status
import palanga.aconcagua
import scalapb.zio_grpc.{ GenericBindable, ServiceList, TransformableService, ZBindableService, ZGeneratedService }
import zio.stream.ZStream
import zio.{ ExitCode, Task, URIO, ZEnv, ZIO }

object GrpcExample extends zio.App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    aconcagua.grpc
      .app(ServiceList add PaintersService add MuseumsService)
      .withPort(9000)
      //      .withDefaultMetrics
      .instrumented
      .run
      .exitCode

  object PaintersService extends ZPainters[Any, Any] {

    override def painterByName(request: PainterRequest): ZIO[Any, Status, Painter] =
      db.painters.read(request.name).mapError(_ => Status.NOT_FOUND.augmentDescription(s"${request.name} not found"))

    override def painters(request: Empty): ZStream[Any, Status, Painter] =
      db.painters.readAll

  }

  object MuseumsService extends ZMuseums[Any, Any] {
    override def museums(request: Empty): ZStream[Any, Status, Museum] =
      db.museums.readAll
  }

  object db {

    type Name     = String
    type Painting = String

    private val painters_data =
      List(
        Painter("Remedios Varo", "La Huida" :: "Mujer Saliendo del Psicoanalista" :: Nil),
        Painter("Claude Monet", "Impression Soleil Levant" :: Nil),
      ).map(p => p.name -> p).toMap

    object painters {
      def read(name: Name): Task[Painter]         = ZIO effect painters_data(name)
      def readAll: ZStream[Any, Nothing, Painter] = ZStream fromIterable painters_data.values
    }

    private val museum_data =
      List(
        Museum("Marmothan Monet", "Impression Soleil Levant" :: Nil),
        Museum("Museo de Arte Moderno de Mexico", "La Huida" :: "Mujer Saliendo del Psicoanalista" :: Nil),
      )

    object museums {
      def readAll: ZStream[Any, Nothing, Museum] = ZStream fromIterable museum_data
    }

  }

}
