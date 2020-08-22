package io.orangebuffalo.simpleaccounting.domain.invoices

import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableJdbcRepositories
class InvoicesConfiguration
