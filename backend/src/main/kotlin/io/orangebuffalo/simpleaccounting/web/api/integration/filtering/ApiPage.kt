package io.orangebuffalo.simpleaccounting.web.api.integration.filtering

data class ApiPage<out T>(
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val data: List<T>
)
