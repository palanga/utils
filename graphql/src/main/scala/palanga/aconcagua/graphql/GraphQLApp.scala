package palanga.aconcagua.graphql

import caliban.GraphQL
import caliban.wrappers.Wrapper.OverallWrapper
import palanga.aconcagua
import palanga.aconcagua.graphql.GraphQLApp.metrics
import zio.{ Has, Runtime }

import scala.concurrent.ExecutionContext

case class GraphQLApp[R <: Has[_]](
  private val api: GraphQL[R],
  private val host: String = "localhost",
  private val port: Int = 8088,
  private val executionContext: Runtime[_] => ExecutionContext = _.platform.executor.asEC,
) extends aconcagua.App[R] {

  override def run = server.run(api, host, port, executionContext)

  override def withPort(port: Int) = copy(port = port)
  override def withDefaultMetrics  = copy(api = api withWrapper metrics)

  def withHost(host: String)                                   = copy(host = host)
  def withExecutionContext(ec: Runtime[_] => ExecutionContext) = copy(executionContext = ec)

}

object GraphQLApp {
  private val metrics: OverallWrapper[Any] = {
    import zio.zmx.metrics._
    OverallWrapper(process => process(_).counted(aconcagua.metrics.TOTAL_REQUESTS))
  }
}
