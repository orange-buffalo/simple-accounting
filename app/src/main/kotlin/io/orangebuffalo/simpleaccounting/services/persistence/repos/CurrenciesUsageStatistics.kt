package io.orangebuffalo.simpleaccounting.services.persistence.repos

data class CurrenciesUsageStatistics(
    val currency: String,
    val count: Long
)
