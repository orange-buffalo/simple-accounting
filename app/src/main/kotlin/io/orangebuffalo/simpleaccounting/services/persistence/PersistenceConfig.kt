package io.orangebuffalo.simpleaccounting.services.persistence

import io.orangebuffalo.simpleaccounting.infra.jooq.set
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.core.convert.JdbcConverter
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableJdbcRepositories(basePackages = ["io.orangebuffalo.simpleaccounting"])
@EnableTransactionManagement
class PersistenceConfig {

    @Bean
    fun connectionProvider(dataSource: DataSource): DataSourceConnectionProvider =
        DataSourceConnectionProvider(TransactionAwareDataSourceProxy(dataSource))

    @Bean
    fun dslContext(
        dataSourceConnectionProvider: DataSourceConnectionProvider,
        jdbcConverter: JdbcConverter,
        jdbcMappingContext: JdbcMappingContext
    ): DSLContext = DSL.using(
        DefaultConfiguration()
            .set(SQLDialect.H2)
            .set(dataSourceConnectionProvider)
            .set(jdbcConverter)
            .set(jdbcMappingContext)
    )
}
