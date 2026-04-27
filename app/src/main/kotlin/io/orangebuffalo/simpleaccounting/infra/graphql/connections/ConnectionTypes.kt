package io.orangebuffalo.simpleaccounting.infra.graphql.connections

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName

/**
 * Pagination constants for cursor-based connections.
 * See [GraphQL Cursor Connections Specification](https://relay.dev/graphql/connections.htm)
 * and [GraphQL Pagination Guide](https://graphql.org/learn/pagination/).
 */
object GraphqlPaginationConstants {
    const val PAGE_SIZE_MIN = 1L
    const val PAGE_SIZE_MAX = 500L
    const val PAGE_SIZE_DEFAULT = 10L
}

/**
 * Generic connection DTO for cursor-based pagination following the
 * [GraphQL Cursor Connections Specification](https://relay.dev/graphql/connections.htm).
 *
 * graphql-kotlin does not support generic type parameters in schema generation,
 * so [ConnectionSchemaGenerationSupport] intercepts this type and generates
 * concrete schema types (e.g., `WorkspacesConnection`, `DocumentsConnection`)
 * based on the node type parameter [N].
 */
@GraphQLIgnore
data class ConnectionGqlDto<N : Any>(
    val edges: List<EdgeGqlDto<N>>,
    val pageInfo: PageInfoGqlDto,
    val totalCount: Int,
)

/**
 * Generic edge DTO for cursor-based pagination.
 * See [ConnectionGqlDto] for details on schema generation.
 */
@GraphQLIgnore
data class EdgeGqlDto<N : Any>(
    val cursor: String,
    val node: N,
)

/**
 * Page info for cursor-based pagination following the
 * [GraphQL Cursor Connections Specification](https://relay.dev/graphql/connections.htm).
 * Reusable across all connection types.
 */
@GraphQLName("PageInfo")
@GraphQLDescription("Pagination information following the GraphQL Cursor Connections Specification.")
data class PageInfoGqlDto(
    @GraphQLDescription("Cursor of the first edge in the page.")
    val startCursor: String?,

    @GraphQLDescription("Cursor of the last edge in the page.")
    val endCursor: String?,

    @GraphQLDescription("Whether there are more items when paginating backwards.")
    val hasPreviousPage: Boolean,

    @GraphQLDescription("Whether there are more items when paginating forwards.")
    val hasNextPage: Boolean,
)

