package io.orangebuffalo.simpleaccounting.infra.graphql.connections

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Pagination constants for cursor-based connections.
 * See [GraphQL Cursor Connections Specification](https://relay.dev/graphql/connections.htm)
 * and [GraphQL Pagination Guide](https://graphql.org/learn/pagination/).
 */
object GraphqlPaginationConstants {
    const val PAGE_SIZE_MIN = 1L
    const val PAGE_SIZE_MAX = 500L
}

/**
 * Page info for cursor-based pagination following the
 * [GraphQL Cursor Connections Specification](https://relay.dev/graphql/connections.htm).
 * Reusable across all connection types.
 */
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

