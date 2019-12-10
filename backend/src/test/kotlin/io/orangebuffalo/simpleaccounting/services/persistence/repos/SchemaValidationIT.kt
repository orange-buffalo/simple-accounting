package io.orangebuffalo.simpleaccounting.services.persistence.repos

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import javax.sql.DataSource
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@Transactional
@TestPropertySource(properties = ["logging.level.org.hibernate.SQL=DEBUG"])
class SchemaValidationIT {

    @Autowired
    @Qualifier("hibernateJdbcTemplate")
    private lateinit var hibernateJdbcTemplate: JdbcTemplate

    @Autowired
    @Qualifier("flywayJdbcTemplate")
    private lateinit var flywayJdbcTemplate: JdbcTemplate

    @Test
    fun `Hibernate and Flyway DDL should be in sync`() {
        val flywayDdl = getDbDdl(flywayJdbcTemplate)
        val hibernateDdl = getDbDdl(hibernateJdbcTemplate)

        assertThat(flywayDdl).isEqualTo(hibernateDdl)
    }

    @TestConfiguration
    class SchemaValidationITConfiguration {

        @Bean(name = ["hibernateDatasource"])
        @Primary
        fun hibernateDatasource(): DataSource {
            return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:hibernate")
                .build()
        }

        @Bean(name = ["hibernateJdbcTemplate"])
        @Primary
        fun jdbcTemplate(hibernateDatasource: DataSource): JdbcTemplate {
            return JdbcTemplate(hibernateDatasource)
        }

        @Bean(name = ["flywayDatasource"])
        @FlywayDataSource
        fun flywayDatasource(): DataSource {
            return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:flyway")
                .build()
        }

        @Bean(name = ["flywayJdbcTemplate"])
        fun flywayJdbcTemplate(@FlywayDataSource flywayDatasource: DataSource): JdbcTemplate {
            return JdbcTemplate(flywayDatasource)
        }
    }
}
