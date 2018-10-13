package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import org.springframework.data.domain.Pageable

class ApiPageRequest(
        val page: Pageable
)