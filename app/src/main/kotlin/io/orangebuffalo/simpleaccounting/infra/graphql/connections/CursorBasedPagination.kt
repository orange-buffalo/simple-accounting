package io.orangebuffalo.simpleaccounting.infra.graphql.connections

import java.time.Instant
import java.time.LocalDate
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

data class ExpenseCursorPage(
    val datePaid: LocalDate?,
    val createdAt: Instant?,
)

fun decodeExpenseCursor(cursor: String?): ExpenseCursorPage {
    if (cursor == null) return ExpenseCursorPage(datePaid = null, createdAt = null)
    val decoded = String(decoder.decode(cursor))
    val separatorIndex = decoded.indexOf(':')
    return ExpenseCursorPage(
        datePaid = LocalDate.ofEpochDay(decoded.substring(0, separatorIndex).toLong()),
        createdAt = Instant.ofEpochMilli(decoded.substring(separatorIndex + 1).toLong()),
    )
}

fun encodeExpenseCursor(datePaid: LocalDate, createdAt: Instant): String {
    return encoder.encodeToString("${datePaid.toEpochDay()}:${createdAt.toEpochMilli()}".toByteArray())
}

