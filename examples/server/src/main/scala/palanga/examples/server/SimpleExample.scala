package palanga.examples.server

import caliban.GraphQL.graphQL
import caliban.schema.GenericSchema
import caliban.{ GraphQL, RootResolver }
import palanga.aconcagua
import zio._
import zio.stream.ZStream

object SimpleExample extends zio.App {

  case class Painter(paintings: Set[Painting], name: Name)

  type Name     = String
  type Painting = String

  case class Queries(
    painter: PainterArgs => ZIO[Any, Throwable, Painter],
    painters: ZStream[Any, Throwable, Painter],
  )

  case class PainterArgs(name: Name)

  object painters {

    def read(name: Name): Task[Painter] = ZIO effect data(name)

    def readAll: ZStream[Any, Nothing, Painter] = ZStream fromIterable data.values

    private val data =
      List(
        Painter(Set("Papilla Estelar"), "Remedios Varo")
      ).map(p => p.name -> p).toMap

  }

  private val queries =
    Queries(
      painters read _.name,
      painters.readAll,
    )

  object ExampleApi extends GenericSchema[Any] {
    val api: GraphQL[Any] =
      graphQL(
        RootResolver(
          queries
        )
      )
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    aconcagua.app
      .graphql(ExampleApi.api)
      .withDefaultMetrics
      .instrumented
      .run
      .exitCode

}
