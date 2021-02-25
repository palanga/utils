package palanga.aconcagua

import zio.{ Has, ZEnv, ZIO }

trait App[-R <: Has[_]] {

  def run: ZIO[ZEnv with R, Throwable, Unit]

  def withPort(port: Int): App[R]
  def withDefaultMetrics: App[R]

  def instrumented: InstrumentedApp[R] = InstrumentedApp(this)

}
