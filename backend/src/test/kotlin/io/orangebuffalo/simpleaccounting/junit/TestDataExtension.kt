package io.orangebuffalo.simpleaccounting.junit

import org.junit.jupiter.api.extension.*
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.support.TransactionTemplate
import java.lang.reflect.Method
import kotlin.reflect.full.memberProperties

private val tablesToTruncate = mutableSetOf<String>()

class TestDataExtension : Extension, ParameterResolver, BeforeEachCallback, InvocationInterceptor {

    override fun beforeEach(extensionContext: ExtensionContext) {
        val applicationContext = SpringExtension.getApplicationContext(extensionContext)
        val jdbcTemplate = applicationContext.getBean(JdbcTemplate::class.java)

        if (tablesToTruncate.isEmpty()) {
            tablesToTruncate.addAll(jdbcTemplate.queryForList("show tables")
                .asSequence()
                .map { it["TABLE_NAME"] as String }
                .filter { it != "flyway_schema_history" }
                .toList())
        }

        jdbcTemplate.execute("set referential_integrity false")

        tablesToTruncate.forEach { jdbcTemplate.execute("""truncate table "$it"""") }

        jdbcTemplate.execute("set referential_integrity true")
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        val isParametrizedTest =
            parameterContext.declaringExecutable.getAnnotation(ParameterizedTest::class.java) != null
        return !isParametrizedTest && TestData::class.java.isAssignableFrom(parameterContext.parameter.type)
    }

    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) = doIntercept(invocation, invocationContext, extensionContext)

    override fun interceptTestTemplateMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) = doIntercept(invocation, invocationContext, extensionContext)

    private fun doIntercept(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        val testData = invocationContext.arguments.asSequence()
            .filter { it is TestData }
            .map { it as TestData }
            .firstOrNull()

        if (testData != null) {
            val applicationContext = SpringExtension.getApplicationContext(extensionContext)

            val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)
            val jdbcAggregateTemplate = applicationContext.getBean(JdbcAggregateTemplate::class.java)
            transactionTemplate.execute {
                testData.generateData().forEach { entity ->
                    jdbcAggregateTemplate.insert(entity)
                }
            }
        }

        invocation.proceed()
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any =
        parameterContext.parameter.type.getDeclaredConstructor().newInstance() as TestData
}

interface TestData {
    fun generateData(): List<Any> =
        this.javaClass.kotlin.memberProperties.asSequence()
            .map {
                it.get(this) ?: throw IllegalStateException("$it is not valid in $this")
            }
            .toList()
}
