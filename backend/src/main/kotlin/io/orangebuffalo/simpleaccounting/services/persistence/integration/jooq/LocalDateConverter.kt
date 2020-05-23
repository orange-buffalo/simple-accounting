package io.orangebuffalo.simpleaccounting.services.persistence.integration.jooq

import org.jooq.impl.AbstractConverter
import java.sql.Date
import java.time.LocalDate

/**
 * Part of workaround for JOOQ timestamps handling.
 * @see InstantConverter
 */
class LocalDateConverter : AbstractConverter<Date, LocalDate>(Date::class.java, LocalDate::class.java) {
    override fun from(databaseObject: Date?): LocalDate? = databaseObject?.toLocalDate()

    override fun to(userObject: LocalDate?): Date? = userObject?.let { Date.valueOf(it) }
}
