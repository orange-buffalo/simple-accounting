package io.orangebuffalo.accounting.simpleaccounting.services.persistence

import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

internal class DatabaseMigrationServiceTest {

    private lateinit var dataSource: DataSource
    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var migrationService: DatabaseMigrationService
    private val multiTenancyService = Mockito.mock(MultiTenancyService::class.java)

    @BeforeEach
    fun setup() {
        dataSource = HikariDataSource().apply {
            jdbcUrl = "jdbc:h2:mem:test-db-" + System.currentTimeMillis()
            driverClassName = "org.h2.Driver"
        }
        jdbcTemplate = JdbcTemplate(dataSource)
        migrationService = DatabaseMigrationService(dataSource, multiTenancyService)

        doReturn(emptyList<String>())
            .`when`(multiTenancyService)
            .getTenantsSchemas()
    }

    @Test
    fun `Should create new admin schema`() {
        migrationService.init()

        val actualSchemas = jdbcTemplate.queryForList("show schemas", String::class.java)
            .map { it.toLowerCase() }

        assertThat(actualSchemas)
            .contains("admin")
    }

    @Test
    fun `Should upgrade tenants schemas`() {
        jdbcTemplate.execute("create schema \"user_42\"")
        doReturn(listOf("user_42"))
            .`when`(multiTenancyService)
            .getTenantsSchemas()
        
        migrationService.init()

        val actualTablesInNewSchema = jdbcTemplate.query("show tables from \"user_42\"") { rs, _ -> rs.getString(1); }
            .map { it.toLowerCase() }

        assertThat(actualTablesInNewSchema)
            .contains("tax")
    }

    @Test
    fun `Should create a new tenant schema on demand`() {
        migrationService.createUserSchema("user_42")

        val actualTablesInNewSchema = jdbcTemplate.query("show tables from \"user_42\"") { rs, _ -> rs.getString(1); }
            .map { it.toLowerCase() }

        assertThat(actualTablesInNewSchema)
            .contains("tax")
    }
}