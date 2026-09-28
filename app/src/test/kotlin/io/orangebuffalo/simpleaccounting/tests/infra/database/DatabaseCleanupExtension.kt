package io.orangebuffalo.simpleaccounting.tests.infra.database

import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
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
                tablesToTruncate.addAll(jdbcTemplate.queryForList(
                    "select tablename from pg_tables where schemaname = current_schema() and tablename <> 'flyway_schema_history'",
                    String::class.java,
                ).filterNotNull())
            }

            if (tablesToTruncate.isNotEmpty()) {
                jdbcTemplate.execute("truncate table ${tablesToTruncate.joinToString()} cascade")
            }
        }
    }
}

private val tablesToTruncate = mutableSetOf<String>()
