package io.orangebuffalo.simpleaccounting.services.persistence.repos

import com.querydsl.core.annotations.QueryProjection

//todo #222: remove projection
data class CurrenciesUsageStatistics @QueryProjection constructor(
    val currency: String,
    val count: Long
)
