package io.orangebuffalo.simpleaccounting.infra.database

import org.junit.jupiter.api.extension.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * An extension that removes all data from the database before every test.
 */
class DatabaseCleanupExtension : Extension, BeforeEachCallback {

    override fun beforeEach(extensionContext: ExtensionContext) {
        val applicationContext = SpringExtension.getApplicationContext(extensionContext)
        val jdbcTemplate = applicationContext.getBean(JdbcTemplate::class.java)
        val transactionManager = applicationContext.getBean(PlatformTransactionManager::class.java)
        val transactionTemplate = TransactionTemplate(transactionManager).also {
            it.transactionManager = transactionManager
            it.propagationBehavior = TransactionTemplate.PROPAGATION_REQUIRES_NEW
        }

        transactionTemplate.execute {
            if (tablesToTruncate.isEmpty()) {
                tablesToTruncate.addAll(jdbcTemplate.queryForList("show tables")
                    .asSequence()
                    .map { it["TABLE_NAME"] as String }
                    .filter { it != "flyway_schema_history" }
                    .toList())
            }

            jdbcTemplate.execute("set referential_integrity false")

            tablesToTruncate.forEach {
                @Suppress("SqlResolve", "SqlSourceToSinkFlow")
                jdbcTemplate.execute("""truncate table "$it"""")
            }

            jdbcTemplate.execute("set referential_integrity true")
        }
    }
}

private val tablesToTruncate = mutableSetOf<String>()
