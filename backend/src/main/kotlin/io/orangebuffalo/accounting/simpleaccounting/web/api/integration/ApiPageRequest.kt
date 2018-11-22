package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import com.querydsl.core.types.Predicate
import org.springframework.data.domain.Pageable

data class ApiPageRequest(
    val page: Pageable,
    val predicate: Predicate
)