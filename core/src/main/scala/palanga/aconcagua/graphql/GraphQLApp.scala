package palanga.aconcagua.graphql

import caliban.GraphQL
import caliban.wrappers.Wrapper.OverallWrapper
import palanga.aconcagua.graphql.GraphQLApp.metrics
import zio.{ Has, Runtime }

import scala.concurrent.ExecutionContext

case class GraphQLApp[R <: Has[_]](
  api: GraphQL[R],
  host: String = "localhost",
  port: Int = 8088,
  executionContext: Runtime[_] => ExecutionContext = _.platform.executor.asEC,
) {

  def withHost(host: String)                                   = copy(host = host)
  def withPort(port: Int)                                      = copy(port = port)
  def withExecutionContext(ec: Runtime[_] => ExecutionContext) = copy(executionContext = ec)
  def instrumented                                             = InstrumentedGraphQLApp(this)
  def withDefaultMetrics                                       = copy(api = api.withWrapper(metrics))

  def run = server.run(api, host, port, executionContext)

}

object GraphQLApp {
  private val metrics: OverallWrapper[Any] = {
    import zio.zmx.metrics._
    OverallWrapper(process => process(_).counted("total_requests"))
  }
}
