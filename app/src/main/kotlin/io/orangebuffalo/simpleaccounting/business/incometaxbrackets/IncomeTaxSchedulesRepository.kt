package io.orangebuffalo.simpleaccounting.business.incometaxbrackets

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import java.time.LocalDate

interface IncomeTaxSchedulesRepository : AbstractEntityRepository<IncomeTaxSchedule> {
    fun findByCountryCodeAndPeriodStartAndPeriodEnd(
        countryCode: String,
        periodStart: LocalDate,
        periodEnd: LocalDate,
    ): List<IncomeTaxSchedule>
}
