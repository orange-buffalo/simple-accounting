package io.orangebuffalo.simpleaccounting.infra.graphql.connections

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore

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
 * Contract for connection DTOs ensuring a consistent structure across all paginated connections.
 * Concrete connection types must override all properties.
 *
 * Note: graphql-kotlin does not support generic type parameters in schema generation,
 * so this uses abstract properties to enforce the contract at compile time.
 * The interface is excluded from the schema via `@GraphQLIgnore`.
 */
@GraphQLIgnore
interface ConnectionGqlDto {
    val edges: List<*>
    val pageInfo: PageInfoGqlDto
    val totalCount: Int
}

/**
 * Contract for edge DTOs ensuring a consistent structure across all paginated edges.
 */
@GraphQLIgnore
interface EdgeGqlDto {
    val cursor: String
    val node: Any
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

