package io.orangebuffalo.simpleaccounting.infra.graphql.connections

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import java.time.Instant
import java.util.Base64

private const val CURSOR_DELIMITER = ":"
private val encoder = Base64.getEncoder()
private val decoder = Base64.getDecoder()

data class CursorPage(
    val createdAtAfter: Instant?,
    val idAfter: Long?,
)

fun decodeCursor(cursor: String?): CursorPage {
    if (cursor == null) return CursorPage(createdAtAfter = null, idAfter = null)
    val decoded = String(decoder.decode(cursor))
    val parts = decoded.split(CURSOR_DELIMITER, limit = 2)
    require(parts.size == 2) { "Invalid cursor format" }
    return CursorPage(
        createdAtAfter = Instant.ofEpochMilli(parts[0].toLong()),
        idAfter = parts[1].toLong(),
    )
}

fun encodeCursor(createdAt: Instant, id: Long): String {
    val raw = "${createdAt.toEpochMilli()}$CURSOR_DELIMITER$id"
    return encoder.encodeToString(raw.toByteArray())
}

/**
 * Builds a connection from a list of entities. Fetches `requestedPageSize + 1` items to
 * determine if there is a next page.
 *
 * @param items the items fetched from the database (should be `requestedPageSize + 1` max)
 * @param requestedPageSize the `first` argument from the query
 * @param totalCount total number of items matching the filter
 * @param cursorPage the decoded cursor from the `after` argument
 * @param mapper maps each entity to an edge (cursor + node DTO)
 * @param connectionFactory constructs the final connection DTO
 */
fun <T : AbstractEntity, E, C> buildConnection(
    items: List<T>,
    requestedPageSize: Int,
    totalCount: Int,
    cursorPage: CursorPage,
    mapper: (T) -> E,
    connectionFactory: (edges: List<E>, pageInfo: PageInfoGqlDto, totalCount: Int) -> C,
): C {
    val hasNextPage = items.size > requestedPageSize
    val pageItems = if (hasNextPage) items.dropLast(1) else items

    val edges = pageItems.map(mapper)

    val startCursor = pageItems.firstOrNull()?.let { encodeCursor(it.createdAt!!, it.id!!) }
    val endCursor = pageItems.lastOrNull()?.let { encodeCursor(it.createdAt!!, it.id!!) }

    val pageInfo = PageInfoGqlDto(
        startCursor = startCursor,
        endCursor = endCursor,
        hasPreviousPage = cursorPage.createdAtAfter != null,
        hasNextPage = hasNextPage,
    )

    return connectionFactory(edges, pageInfo, totalCount)
}

