package io.orangebuffalo.accounting.simpleaccounting.services.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

private const val USER_SCHEMA_PREFIX = "user_"

@Service
class MultiTenancyService(private val jdbcTemplate: JdbcTemplate) {

    fun getTenantsSchemas(): List<String> =
        jdbcTemplate
            .queryForList("SHOW SCHEMAS", String::class.java)
            .filter { it.startsWith(USER_SCHEMA_PREFIX, ignoreCase = true) }

}