package io.orangebuffalo.simpleaccounting.services.persistence

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
class PersistenceConfig {

    @Bean
    fun dslContext(dataSource: DataSource): DSLContext = DSL.using(dataSource, SQLDialect.H2, Settings())

}
