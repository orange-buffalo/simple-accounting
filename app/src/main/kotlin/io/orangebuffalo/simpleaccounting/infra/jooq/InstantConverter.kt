package io.orangebuffalo.simpleaccounting.infra.jooq

import org.jooq.impl.AbstractConverter
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class InstantConverter : AbstractConverter<OffsetDateTime, Instant>(OffsetDateTime::class.java, Instant::class.java) {
    override fun from(databaseObject: OffsetDateTime?): Instant? = databaseObject?.toInstant()

    override fun to(userObject: Instant?): OffsetDateTime? = userObject?.atOffset(ZoneOffset.UTC)
}
