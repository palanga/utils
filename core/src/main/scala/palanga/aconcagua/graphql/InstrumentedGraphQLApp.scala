package palanga.aconcagua.graphql

import InstrumentedGraphQLApp.defaultPrometheusInstrumentation
import uzhttp.Response
import uzhttp.server.Server
import zio.stream.ZSink
import zio.zmx.metrics.{ Instrumentation, ZMX }
import zio.zmx.prometheus.{ PMetric, PrometheusConfig, PrometheusInstrumentaion, PrometheusRegistry }
import zio.{ Chunk, Has, ZEnv, ZIO }

import java.net.InetSocketAddress
import scala.language.postfixOps

case class InstrumentedGraphQLApp[R <: Has[_]](
  appBuilder: GraphQLApp[R],
  host: String = "localhost",
  port: Int = 8080,
) {

  def withDefaultMetrics            = copy(appBuilder = appBuilder.withDefaultMetrics)
  def withMetricsHost(host: String) = copy(host = host)
  def withMetricsPort(port: Int)    = copy(port = port)

  def run: ZIO[ZEnv with R, Throwable, (Unit, Unit)] = {

    import zio.duration._

    def eventSink(inst: Instrumentation) = ZSink.foreach(inst.handleMetric)

    for {
      instrumentation <- defaultPrometheusInstrumentation
      metrics         <- ZMX.channel.eventStream.run(eventSink(instrumentation)).fork
      result          <- appBuilder.run <&> metricsServer(instrumentation)
      _               <- ZMX.channel.flushMetrics(10 seconds)
      _               <- metrics.interrupt
    } yield result

  }

  private def metricsServer(instrumentation: Instrumentation) =
    Server
      .builder(new InetSocketAddress(host, port))
      .handleAll(_.uri.getPath match {
        case "/metrics" =>
          instrumentation.report.map(_.getOrElse("")).map(Response.plain(_))
        case _          =>
          ZIO.succeed(Response.html("<html><title>Metrics Server</title><a href=\"/metrics\">Metrics</a></html>"))
      })
      .serve
      .use(_.awaitShutdown)

}

object InstrumentedGraphQLApp {

  // TODO prometheus provides a default config ?
  private val config =
    PrometheusConfig(
      buckets = Chunk(_ => Some(PMetric.Buckets.Linear(10, 10, 10))),
      quantiles = Chunk(_ => Some(PrometheusConfig.defaultQuantiles)),
    )

  private val defaultPrometheusInstrumentation = PrometheusRegistry.make(config).map(new PrometheusInstrumentaion(_))

}
