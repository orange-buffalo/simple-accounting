package io.orangebuffalo.simpleaccounting.web.api.integration.filtering

data class ApiPage<out T>(
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val data: List<T>
)

/**
 * Base class for DTOs that encapsulate query parameters for filtering requests.
 * Strong typing allows to generate detailed OpenAPI definition and have type-safe bindings on the client side.
 */
abstract class ApiPageRequest<SF : Enum<SF>> {
    /**
     * Page number, 1-based.
     */
    var pageNumber: Int? = null

    var pageSize: Int? = null

    var sortOrder: ApiPageRequestSortOrder? = null

    // workaround for https://github.com/springdoc/springdoc-openapi/issues/1128
    abstract var sortBy: SF?
}

/**
 * Indicator that sorting is not supported by the filtering API
 */
enum class NoOpFiltering {
    @Suppress("EnumEntryName", "unused")
    _NOT_SUPPORTED
}

@Suppress("EnumEntryName")
enum class ApiPageRequestSortOrder {
    asc,
    desc
}
