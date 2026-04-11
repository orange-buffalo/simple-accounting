package io.orangebuffalo.simpleaccounting.infra.graphql.connections

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
