package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

class ApiPage<out T>(
        val pageNumber: Int,
        val pageSize: Int,
        val totalElements: Long,
        val data: List<T>
)
