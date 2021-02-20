package palanga.aconcagua

import caliban.GraphQL
import palanga.aconcagua.graphql.GraphQLApp
import palanga.aconcagua.grpc.GrpcApp
import scalapb.zio_grpc.{ ServiceList, ZBindableService }
import zio.Has

object app {

  def grpc[R <: Has[_], S](service: S)(implicit b: ZBindableService[R, S]) = GrpcApp(ServiceList.add(service))

  def grpc[R <: Has[_], S](serviceList: ServiceList[R]) = GrpcApp(serviceList)

  /**
   * Create a graphql server from an api definition
   */
  def graphql[R <: Has[_]](api: GraphQL[R]): GraphQLApp[R] = GraphQLApp(api)

}
