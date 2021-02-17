package palanga.aconcagua

import caliban.GraphQL
import palanga.aconcagua.graphql.GraphQLApp
import zio.Has

object app {

  /**
   * Create a graphql server from an api definition
   */
  def graphql[R <: Has[_]](api: GraphQL[R]): GraphQLApp[R] = GraphQLApp(api)

}
