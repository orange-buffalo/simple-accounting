package io.orangebuffalo.simpleaccounting.infra.graphql.connections

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import java.time.Instant
import java.util.Base64

private val encoder = Base64.getEncoder()
private val decoder = Base64.getDecoder()

data class CursorPage(
    val createdAtAfter: Instant?,
)

fun decodeCursor(cursor: String?): CursorPage {
    if (cursor == null) return CursorPage(createdAtAfter = null)
    val decoded = String(decoder.decode(cursor))
    return CursorPage(
        createdAtAfter = Instant.ofEpochMilli(decoded.toLong()),
    )
}

fun encodeCursor(createdAt: Instant): String {
    return encoder.encodeToString(createdAt.toEpochMilli().toString().toByteArray())
}

/**
 * Builds a connection from a list of entities. Fetches `requestedPageSize + 1` items to
 * determine if there is a next page.
 *
 * @param items the items fetched from the database (should be `requestedPageSize + 1` max)
 * @param requestedPageSize the `first` argument from the query
 * @param totalCount total number of items matching the filter
 * @param cursorPage the decoded cursor from the `after` argument
 * @param mapper maps each entity to an edge DTO
 */
fun <T : AbstractEntity, N : Any> buildConnection(
    items: List<T>,
    requestedPageSize: Int,
    totalCount: Int,
    cursorPage: CursorPage,
    mapper: (T) -> EdgeGqlDto<N>,
): ConnectionGqlDto<N> {
    val hasNextPage = items.size > requestedPageSize
    val pageItems = if (hasNextPage) items.dropLast(1) else items

    val edges = pageItems.map(mapper)

    val startCursor = pageItems.firstOrNull()?.let { encodeCursor(it.createdAt!!) }
    val endCursor = pageItems.lastOrNull()?.let { encodeCursor(it.createdAt!!) }

    val pageInfo = PageInfoGqlDto(
        startCursor = startCursor,
        endCursor = endCursor,
        hasPreviousPage = cursorPage.createdAtAfter != null,
        hasNextPage = hasNextPage,
    )

    return ConnectionGqlDto(edges = edges, pageInfo = pageInfo, totalCount = totalCount)
}

