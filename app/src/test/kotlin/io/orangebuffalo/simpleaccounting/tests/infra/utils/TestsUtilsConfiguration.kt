package io.orangebuffalo.simpleaccounting.tests.infra.utils

import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactoryInfra
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
    fun entitiesFactoryInfra(
        platformTransactionManager: PlatformTransactionManager,
        jdbcAggregateTemplate: JdbcAggregateTemplate
    ) = EntitiesFactoryInfra(
        platformTransactionManager = platformTransactionManager,
        jdbcAggregateTemplate = jdbcAggregateTemplate
    )
}
