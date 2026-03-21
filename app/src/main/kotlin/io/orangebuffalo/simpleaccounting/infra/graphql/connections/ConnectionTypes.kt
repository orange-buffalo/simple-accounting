package io.orangebuffalo.simpleaccounting.infra.graphql.connections

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Page info for cursor-based pagination following the GraphQL Cursor Connections Specification.
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

