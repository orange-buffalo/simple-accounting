package io.orangebuffalo.simpleaccounting.infra.jooq

import org.jooq.impl.AbstractConverter
import java.sql.Timestamp
import java.time.Instant

/**
 * JOOQ implementation if Instant binding relies on string output parsing. It does not work properly with H2.
 * We have to disable Java 8 Time types and manually convert from SQL types into time types.
 */
class InstantConverter : AbstractConverter<Timestamp, Instant>(Timestamp::class.java, Instant::class.java) {
    override fun from(databaseObject: Timestamp?): Instant? = databaseObject?.toInstant()

    override fun to(userObject: Instant?): Timestamp? = userObject?.let { Timestamp.from(it) }
}
