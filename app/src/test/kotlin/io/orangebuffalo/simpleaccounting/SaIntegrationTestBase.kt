package io.orangebuffalo.simpleaccounting

import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClientConfig
import io.orangebuffalo.simpleaccounting.tests.infra.database.DatabaseCleanupExtension
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactoryInfra
import io.orangebuffalo.simpleaccounting.tests.infra.utils.mockCurrentTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.PlatformTransactionManager
import kotlin.reflect.KProperty

/**
 * Base class for all test that verify the functionality on the full Spring context.
 * Provides various testing facilities.
 */
@ExtendWith(
    SpringExtension::class,
    DatabaseCleanupExtension::class,
)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = ["spring.profiles.active=test"])
@Import(
    ApiTestClientConfig::class,
)
abstract class SaIntegrationTestBase {

    @Autowired
    protected lateinit var aggregateTemplate: JdbcAggregateTemplate

    // TODO make private when legacy tests are migrated to preconditions usage
    protected lateinit var entitiesFactoryInfra: EntitiesFactoryInfra

    @MockitoSpyBean
    protected lateinit var timeService: TimeService

    @MockitoBean
    protected lateinit var passwordEncoder: PasswordEncoder

    @MockitoSpyBean
    protected lateinit var tokenGenerator: TokenGenerator

    @Autowired
    private lateinit var platformTransactionManager: PlatformTransactionManager

    private val lazyPreconditions = mutableListOf<LazyRepeatablePreconditionsDelegate<*>>()

    @BeforeEach
    private fun setupSaIntegrationTestBase() {
        entitiesFactoryInfra = EntitiesFactoryInfra(
            platformTransactionManager = platformTransactionManager,
            jdbcAggregateTemplate = aggregateTemplate,
        )

        lazyPreconditions.forEach { it.reset() }

        // Mock the password encoder to speed up login process and control it easily in the tests.
        // TODO remove from base class when full stack tests use JWT token test auth
        whenever(passwordEncoder.matches(any(), any())) doReturn true
        whenever(passwordEncoder.encode(any())) doAnswer { it.arguments[0] as String }

        // Mock the time service to return a consistent time for tests
        mockCurrentTime(timeService)
    }

    /**
     * Sets up the database (domain model) preconditions for the test.
     * Useful when each test needs to set up its own preconditions.
     * For shared preconditions, consider using [lazyPreconditions] instead.
     *
     * Often, this method will return an `object` with fields, to allow safe
     * access to the preconditions in the test methods, e.g.:
     * ```kotlin
     * class MyTest : SaIntegrationTestBase() {
     *   @Test
     *   fun myTest() {
     *     val preconditions = preconditions {
     *       object {
     *         val myEntity = createMyEntity()
     *         val myOtherEntity = createMyOtherEntity()
     *       }
     *     preconditions.myEntity
     *   }
     * }
     */
    protected fun <T> preconditions(spec: EntitiesFactory.() -> T): T = spec(createEntitiesFactory())

    /**
     * Sets up the database (domain model) preconditions for the test.
     * This is a lazy version of [preconditions] that allows
     * reusing the same preconditions across multiple tests. The preconditions
     * are guaranteed to be created once per test method execution,
     * when `value` is accessed for the first time.
     *
     * Typically, this will be declared as a test field delegate, like this:
     * ```kotlin
     * class MyTest : SaIntegrationTestBase() {
     *    private val myPreconditions by lazyPreconditions {
     *      object {
     *         val myEntity = createMyEntity()
     *      }
     *    }
     *
     *    @Test
     *    fun myTest() {
     *       myPreconditions.myEntity
     *    }
     * }
     * ```
     */
    protected fun <P> lazyPreconditions(
        spec: EntitiesFactory.() -> P
    ): LazyRepeatablePreconditionsDelegate<P> {
        val delegate = LazyRepeatablePreconditionsDelegate(spec)
        lazyPreconditions.add(delegate)
        return delegate
    }

    private fun createEntitiesFactory(): EntitiesFactory {
        return EntitiesFactory(entitiesFactoryInfra)
    }

    protected inner class LazyRepeatablePreconditionsDelegate<P>(
        private val preconditionsSpec: EntitiesFactory.() -> P,
    ) {
        private var preconditions: P? = null

        internal fun reset() {
            preconditions = null
        }

        /**
         * Gets preconditions, creating them if necessary.
         */
        operator fun getValue(thisRef: Any?, property: KProperty<*>): P {
            if (preconditions == null) {
                preconditions = preconditionsSpec(createEntitiesFactory())
            }
            return preconditions!!
        }
    }
}
