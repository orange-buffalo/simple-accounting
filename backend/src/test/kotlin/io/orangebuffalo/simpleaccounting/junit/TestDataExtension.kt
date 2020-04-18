package io.orangebuffalo.simpleaccounting.junit

import io.orangebuffalo.simpleaccounting.currentEntityId
import io.orangebuffalo.simpleaccounting.services.persistence.entities.LegacyAbstractEntity
import org.flywaydb.core.Flyway
import org.hibernate.ReplicationMode
import org.hibernate.Session
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.support.TransactionTemplate
import java.lang.reflect.Method
import javax.persistence.EntityManager
import kotlin.reflect.full.memberProperties

class TestDataExtension : Extension, ParameterResolver, BeforeEachCallback, InvocationInterceptor {

    override fun beforeEach(extensionContext: ExtensionContext) {
        val applicationContext = SpringExtension.getApplicationContext(extensionContext)

        val flyway = applicationContext.getBean(Flyway::class.java)
        flyway.clean()
        flyway.migrate()
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
            val entityManager = applicationContext.getBean(EntityManager::class.java)
            val jdbcAggregateTemplate = applicationContext.getBean(JdbcAggregateTemplate::class.java)
            transactionTemplate.execute {
                val session = entityManager.unwrap(Session::class.java)
                val hibernateMetaModel = session.sessionFactory.metamodel
                testData.generateData().forEach { entity ->
                    try {
                        //todo #222: cleanup
                        hibernateMetaModel.entity(entity::class.java)
                        val abstractEntity = entity as LegacyAbstractEntity
                        if (abstractEntity.id == null) {
                            abstractEntity.id = currentEntityId++
                            abstractEntity.version = 0
                        }
                        session.replicate(entity, ReplicationMode.EXCEPTION)
                        entityManager.flush()
                    } catch (e: IllegalArgumentException) {
                        jdbcAggregateTemplate.insert(entity)
                    }
                }
            }
        }

        invocation.proceed()
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any =
        parameterContext.parameter.type.newInstance() as TestData
}

interface TestData {
    fun generateData(): List<Any> =
        this.javaClass.kotlin.memberProperties.asSequence()
            .map {
                it.get(this) ?: throw IllegalStateException("$it is not valid in $this")
            }
            .toList()
}
