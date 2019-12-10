package io.orangebuffalo.simpleaccounting.junit

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.extension.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.EntityManager
import kotlin.reflect.full.memberProperties

class TestDataExtension : Extension, ParameterResolver, BeforeEachCallback {

    override fun beforeEach(extensionContext: ExtensionContext) {
        val applicationContext = SpringExtension.getApplicationContext(extensionContext)

        val flyway = applicationContext.getBean(Flyway::class.java)
        flyway.clean()
        flyway.migrate()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        TestData::class.java.isAssignableFrom(parameterContext.parameter.type)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val testData = parameterContext.parameter.type.newInstance() as TestData

        val applicationContext = SpringExtension.getApplicationContext(extensionContext)

        val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)
        val entityManager = applicationContext.getBean(EntityManager::class.java)
        transactionTemplate.execute {
            testData.generateData().forEach(entityManager::persist)
        }

        return testData
    }
}

interface TestData {
    fun generateData(): List<Any> =
        this.javaClass.kotlin.memberProperties.asSequence()
            .map {
                it.get(this) ?: throw IllegalStateException("$it is not valid in $this")
            }
            .toList()
}
