package io.orangebuffalo.simpleaccounting.infra.database

import org.junit.jupiter.api.extension.*
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * An extension that provides a way to create and manage test data in the database.
 *
 * Injects [TestDataFactory] instances into the test instances or methods. See factory docs
 * for more details on the intended usage.
 */
class TestDataExtension : Extension, ParameterResolver, BeforeEachCallback {

    override fun beforeEach(extensionContext: ExtensionContext) {
        cleanupDatabase(extensionContext)
    }

    private fun cleanupDatabase(extensionContext: ExtensionContext) {
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

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        TestDataFactory::class.java == parameterContext.parameter.type

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        if (extensionContext.testMethod.isEmpty) {
            throw ParameterResolutionException("TestDataFactory can only be injected into methods")
        }
        val applicationContext = SpringExtension.getApplicationContext(extensionContext)
        val jdbcAggregateTemplate = applicationContext.getBean(JdbcAggregateTemplate::class.java)
        val transactionManager = applicationContext.getBean(PlatformTransactionManager::class.java)
        val testDataFactory = TestDataFactory(
            platformTransactionManager = transactionManager,
            jdbcAggregateTemplate = jdbcAggregateTemplate
        )
        return testDataFactory
    }
}

private val tablesToTruncate = mutableSetOf<String>()
