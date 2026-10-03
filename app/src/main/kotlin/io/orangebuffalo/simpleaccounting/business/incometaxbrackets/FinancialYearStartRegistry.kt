package io.orangebuffalo.simpleaccounting.business.incometaxbrackets

import java.time.MonthDay

object FinancialYearStartRegistry {
    val byResidency: Map<String, MonthDay> = mapOf(
        "AU" to MonthDay.of(7, 1),
        "US" to MonthDay.of(1, 1),
        "CA" to MonthDay.of(1, 1),
        "GB" to MonthDay.of(4, 6),
        "NZ" to MonthDay.of(4, 1),
        "IE" to MonthDay.of(1, 1),
        "ZA" to MonthDay.of(3, 1),
    )
}
