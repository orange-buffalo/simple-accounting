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
class TestDataExtension : Extension, ParameterResolver, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    override fun beforeEach(extensionContext: ExtensionContext) {
        cleanupDatabase(extensionContext)
        setupTestData(extensionContext)
    }

    private fun setupTestData(extensionContext: ExtensionContext) {
        extensionContext.testDataFactories.forEach { it.setup() }
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
        val applicationContext = SpringExtension.getApplicationContext(extensionContext)
        val jdbcAggregateTemplate = applicationContext.getBean(JdbcAggregateTemplate::class.java)
        val transactionManager = applicationContext.getBean(PlatformTransactionManager::class.java)
        val testDataFactory = TestDataFactory(
            platformTransactionManager = transactionManager,
            jdbcAggregateTemplate = jdbcAggregateTemplate
        )
        extensionContext.testDataFactories.add(testDataFactory)
        return testDataFactory
    }

    override fun afterEach(context: ExtensionContext) {
        // reset the factories to re-create the data for the next test
        context.testDataFactories.forEach { it.reset() }
    }

    override fun afterAll(context: ExtensionContext) {
        // remove the factories from the store to avoid memory leaks
        context.getStore(testDataStoreNs).remove(testDataStoreKey)
    }
}

private val ExtensionContext.testDataFactories: MutableList<TestDataFactory>
    get() {
        val store = this.getStore(testDataStoreNs)

        @Suppress("UNCHECKED_CAST")
        var factoriesPerInstance =
            store.get(testDataStoreKey, MutableMap::class.java) as MutableMap<Any, MutableList<TestDataFactory>>?
        if (factoriesPerInstance == null) {
            factoriesPerInstance = mutableMapOf()
            store.put(testDataStoreKey, factoriesPerInstance)
        }

        val currentInstance = this.testInstance.orElse(emptyInstanceToken)
        return factoriesPerInstance.getOrPut(currentInstance) { mutableListOf() }
    }

private val tablesToTruncate = mutableSetOf<String>()
private val testDataStoreNs = ExtensionContext.Namespace.create(TestDataFactory::class.qualifiedName)
private const val testDataStoreKey = "testDataFactories"

// in order to support constructor injection, when the test instance is not yet available
private val emptyInstanceToken = Any()
