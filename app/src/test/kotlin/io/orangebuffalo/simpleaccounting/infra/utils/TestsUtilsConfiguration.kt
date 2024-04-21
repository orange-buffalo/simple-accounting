package io.orangebuffalo.simpleaccounting.infra.utils

import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.transaction.PlatformTransactionManager

/**
 * Configuration for various testing utilities.
 */
@Configuration
class TestsUtilsConfiguration {

    @Bean
    fun preconditionsInfra(
        platformTransactionManager: PlatformTransactionManager,
        jdbcAggregateTemplate: JdbcAggregateTemplate
    ) = PreconditionsInfra(
        platformTransactionManager = platformTransactionManager,
        jdbcAggregateTemplate = jdbcAggregateTemplate
    )
}
