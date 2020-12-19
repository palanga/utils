package palanga.caliban.http4s

import caliban.wrappers.Wrapper.OverallWrapper
import caliban.{ GraphQL, Http4sAdapter }
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.console.{ putStrLnErr, Console }
import zio.interop.catz._
import zio.{ Chunk, Has, RIO, Runtime, ZEnv, ZIO }

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

object server {

  def run[R <: Has[_]](
    api: GraphQL[R],
    host: String = "localhost",
    port: Int = 8088,
    executionContext: Runtime[_] => ExecutionContext = _.platform.executor.asEC,
  ): ZIO[ZEnv with R, Throwable, Unit] = {
    type RTask[A] = RIO[ZEnv with R, A]
    ZIO
      .runtime[ZEnv with R]
      .flatMap { implicit runtime =>
        for {
          _           <- zio.console putStrLn api.render
          interpreter <- api.withWrapper(printErrors).interpreter
          _           <- BlazeServerBuilder[RTask](executionContext(runtime))
                 .bindHttp(port, host)
                 .withHttpApp(
                   Router[RTask](
                     "/api/graphql" -> CORS(Http4sAdapter makeHttpService interpreter),
                     "/ws/graphql"  -> CORS(Http4sAdapter makeWebSocketService interpreter),
                   ).orNotFound
                 )
                 .resource
                 .toManaged
                 .useForever
        } yield ()
      }
  }

  private val printErrors: OverallWrapper[Console] =
    OverallWrapper { process => request =>
      process(request).tap(response =>
        ZIO.when(response.errors.nonEmpty)(
          putStrLnErr(response.errors.flatMap(prettyStackStrace).mkString("", "\n", "\n"))
        )
      )
    }

  private def prettyStackStrace(t: Throwable): Chunk[String] = {
    @tailrec def go(acc: Chunk[String], t: Throwable): Chunk[String] =
      if (t == null) acc
      else go(acc ++ (t.toString +: Chunk.fromArray(t.getStackTrace).map("\tat " + _.toString)), t.getCause)
    go(Chunk(""), t)
  }

}
