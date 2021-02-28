package examples

import examples.painter.ZioPainter.{ ZMuseums, ZPainters }
import examples.painter.{ Empty, Museum, Painter, PainterRequest }
import io.grpc.Status
import palanga.aconcagua
import palanga.zio.eventsourcing.EventSource.EventSource
import palanga.zio.eventsourcing.journal.Journal
import palanga.zio.eventsourcing.journal.cassandra.CassandraJournal.Codec
import palanga.zio.eventsourcing.{ journal, ApplyEvent, EventSource }
import scalapb.zio_grpc.ServiceList
import zio.console.Console
import zio.json._
import zio.{ ExitCode, URIO, ZEnv }

import java.util.UUID
import scala.language.experimental.macros

object GrpcEventSourcingExample extends zio.App {

  import examples.GrpcEventSourcingExample.Event._

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    aconcagua.grpc
      .app(ServiceList add Painters add Museums)
      .withPort(9000)
      .withDefaultMetrics
      .instrumented
      .run
      .provideCustomLayer(testingLayer)
      .exitCode

  implicit private val uuidEncoder = JsonEncoder.string.contramap[UUID](_.toString)
  implicit private val uuidDecoder = JsonDecoder.string.map[UUID](UUID.fromString)

  implicit private val museumEncoder  = DeriveJsonEncoder.gen[MuseumEvent]
  implicit private val museumDecoder  = DeriveJsonDecoder.gen[MuseumEvent]
  implicit private val painterEncoder = DeriveJsonEncoder.gen[PainterEvent]
  implicit private val painterDecoder = DeriveJsonDecoder.gen[PainterEvent]

  implicit private val paintersCodec: Codec[PainterEvent] =
    Codec(_.toJson, _.fromJson[PainterEvent].left.map(new Exception(_)))

  implicit private val museumCodec: Codec[MuseumEvent] =
    Codec(_.toJson, _.fromJson[MuseumEvent].left.map(new Exception(_)))

  private val cassandraSession    = palanga.zio.cassandra.ZCqlSession.layer.default
  private val paintersJournal     = journal.cassandra.test[PainterEvent].tap(j => initializePainters(j.get))
  private val museumJournal       = journal.cassandra.test[MuseumEvent].tap(j => initializeMuseums(j.get))
  private val paintersEventSource = EventSource.live(painterApplyEvent)
  private val museumsEventSource  = EventSource.live(museumApplyEvent)

  private val testingLayer =
    cassandraSession >>> (paintersJournal >>> paintersEventSource) ++ (museumJournal >>> museumsEventSource)

  private def initializePainters(journal: Journal.Service[PainterEvent]) =
    journal
      .write(UUID.randomUUID() -> PainterEvent.PainterCreated("Remedios Varo"))
      .flatMap(e => journal.write(e._1 -> PainterEvent.PaintingAdded("La Huida", e._1)))
      .flatMap(e => journal.write(e._1 -> PainterEvent.PaintingAdded("Mujer Saliendo del Psicoanalista", e._1))) *>
      journal
        .write(UUID.randomUUID() -> PainterEvent.PainterCreated("Claude Monet"))
        .flatMap(e => journal.write(e._1 -> PainterEvent.PaintingAdded("Impression Soleil Levant", e._1)))

  private def initializeMuseums(journal: Journal.Service[MuseumEvent])   =
    journal
      .write(UUID.randomUUID() -> MuseumEvent.MuseumCreated("Museo de Arte Moderno de Mexico"))
      .flatMap(e => journal.write(e._1 -> MuseumEvent.PaintingAdded("La Huida", e._1)))
      .flatMap(e => journal.write(e._1 -> MuseumEvent.PaintingAdded("Mujer Saliendo del Psicoanalista", e._1))) *>
      journal
        .write(UUID.randomUUID() -> MuseumEvent.MuseumCreated("Marmothan Monet"))
        .flatMap(e => journal.write(e._1 -> MuseumEvent.PaintingAdded("Impression Soleil Levant", e._1)))

  sealed trait Event
  object Event {

    sealed trait PainterEvent extends Event
    object PainterEvent {
      case class PainterCreated(name: String)                 extends PainterEvent
      case class PaintingAdded(name: String, painterId: UUID) extends PainterEvent
    }

    sealed trait MuseumEvent extends Event
    object MuseumEvent {
      case class MuseumCreated(name: String)                 extends MuseumEvent
      case class PaintingAdded(name: String, museumId: UUID) extends MuseumEvent
    }

  }

  def painterApplyEvent: ApplyEvent[Painter, PainterEvent] = {
    case (None, PainterEvent.PainterCreated(name))                =>
      Right(Painter(name))
    case (Some(painter), PainterEvent.PaintingAdded(painting, _)) =>
      Right(painter addPaintings painting)
    case (p, e)                                                   =>
      Left(new Exception(s"invalid event $e on painter $p"))
  }

  def museumApplyEvent: ApplyEvent[Museum, MuseumEvent] = {
    case (None, MuseumEvent.MuseumCreated(name))                =>
      Right(Museum(name))
    case (Some(museum), MuseumEvent.PaintingAdded(painting, _)) =>
      Right(museum addPaintings painting)
    case (p, e)                                                 =>
      Left(new Exception(s"invalid event $e on museum $p"))
  }

  object Painters extends ZPainters[EventSource[Painter, PainterEvent] with Console, Any] {

    override def painterByName(request: PainterRequest) =
      painters(Empty.defaultInstance)
        .filter(_.name == request.name)
        .runHead
        .someOrFail(Status.NOT_FOUND.augmentDescription(s"Couldn't find painter named <<${request.name}>>"))

    override def painters(request: Empty) =
      EventSource
        .of[Painter, PainterEvent]
        .readAll
        .onError(c => zio.console.putStrLnErr(c.prettyPrint))
        .mapError(_ => Status.UNKNOWN)
        .map(identity)

  }

  object Museums extends ZMuseums[EventSource[Museum, MuseumEvent] with Console, Any] {

    override def museums(request: Empty) =
      EventSource
        .of[Museum, MuseumEvent]
        .readAll
        .onError(c => zio.console.putStrLnErr(c.prettyPrint))
        .mapError(_ => Status.UNKNOWN)
        .map(identity)

  }

}
