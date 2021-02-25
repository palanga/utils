package palanga.aconcagua

import scalapb.zio_grpc.{ ServiceList, ZBindableService }
import zio.Has

package object grpc {
  def app[R <: Has[_], S](service: S)(implicit b: ZBindableService[R, S]) = GrpcApp(ServiceList add service)
  def app[R <: Has[_], S](serviceList: ServiceList[R])                    = GrpcApp(serviceList)
}
