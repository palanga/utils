package palanga.aconcagua

import caliban.GraphQL
import zio.Has

package object graphql {
  def app[R <: Has[_]](api: GraphQL[R]): GraphQLApp[R] = GraphQLApp(api)
}
