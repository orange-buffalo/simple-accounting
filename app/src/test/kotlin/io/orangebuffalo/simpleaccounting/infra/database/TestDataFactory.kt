package io.orangebuffalo.simpleaccounting.infra.database

import io.orangebuffalo.simpleaccounting.domain.users.I18nSettings
import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import io.orangebuffalo.simpleaccounting.domain.users.UserActivationToken
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import kotlin.reflect.KProperty

/**
 * Factory for creating test data (preconditions) in the database.
 * In fact, it is a synthetic sugar on top of [EntitiesFactory] in order to allow for
 * anonymous inline class instances creation via `object` keyword. This allows to share the test data
 * between the test methods without the excessive boilerplate of dedicated classes. Works best
 * with `@Nested` tests where each such test operates on a single set of preconditions.
 *
 * The invocation of the factory will create a property delegate that returns the object
 * created by the specification of preconditions setup, which in turn is called
 * with [EntitiesFactory] as a receiver in order to create such an object.
 *
 * The factory will re-execute the specification once per each test method, ensuring that the preconditions
 * are always fresh and consistent.
 *
 * This factory should be injected via [TestDataExtension] and never created directly.
 *
 * Example:
 * ```
 * @Nested
 * inner class TheTest(
 *      testDataFactory: TestDataFactory
 * ) {
 *      private val preconditions by testDataFactory {
 *          object {
 *              val entity = <any EntitiesFactory method call>
 *              ...
 *          }
 *      }
 *      .... later in the test
 *      preconditions.entity // this will return the entity created by the factory
 * }
 * ```
 */
class TestDataFactory internal constructor(
    platformTransactionManager: PlatformTransactionManager,
    private val jdbcAggregateTemplate: JdbcAggregateTemplate,
) {

    private val transactionTemplate = TransactionTemplate().apply {
        transactionManager = platformTransactionManager
        propagationBehavior = TransactionTemplate.PROPAGATION_REQUIRES_NEW
    }

    private val delegates = mutableListOf<TestDataFactoryDelegate<*>>()

    /**
     * See class-level description for details.
     *
     * In case [lazy] is set to true, the preconditions will be created only when accessed.
     * Otherwise, they will be created during test method setup (default).
     */
    operator fun <T> invoke(lazy: Boolean = false, spec: EntitiesFactory.() -> T): TestDataFactoryDelegate<T> =
        TestDataFactoryDelegate(lazy, spec)
            .also { delegates.add(it) }

    /**
     * Internal for [TestDataExtension] to reset the state between the tests.
     */
    internal fun reset() {
        delegates.forEach { it.reset() }
    }

    /**
     * Internal for [TestDataExtension] to ensure the data is created during the test method setup.
     */
    internal fun setup() {
        delegates.forEach { it.setup() }
    }

    /**
     * Initialized the test data when necessary and returns the object created by the specification.
     * At most, executes the specification once per test method.
     * If is not lazy, the data will be created during the test method setup.
     * Otherwise, it will be created when accessed.
     *
     * See [TestDataFactory] class-level description for details.
     */
    inner class TestDataFactoryDelegate<T>(
        private val lazy: Boolean,
        private val spec: EntitiesFactory.() -> T,
    ) {
        private var initializedData: T? = null

        /**
         * Returns the preconditions object created by the specification,
         * invoking specification if necessary.
         */
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            if (initializedData == null) {
                executeDataCreation()
            }
            return initializedData!!
        }

        /**
         * Internal for [TestDataExtension] to ensure the data is created during the test method setup.
         */
        internal fun setup() {
            if (!lazy) {
                executeDataCreation()
            }
        }

        private fun executeDataCreation() {
            initializedData = transactionTemplate.execute {
                spec(EntitiesFactory())
            }
        }

        /**
         * Internal for [TestDataExtension] to reset the state between the tests.
         */
        internal fun reset() {
            initializedData = null
        }
    }

    /**
     * Factory for creating domain entities in the database.
     * This factory guarantees database consistency for every created entity.
     * In case any of the mandatory fields are not set, the factory will populate them with defaults.
     * This also includes creating and saving any related entities that were not explicitly provided.
     *
     * Tests must explicitly provide any data they depend on and never rely on particular values
     * in the defaults set by the factory.
     */
    inner class EntitiesFactory {
        fun platformUser(
            userName: String = "Farnsworth",
            passwordHash: String = "nopassword",
            isAdmin: Boolean = false,
            documentsStorage: String? = null,
            i18nSettings: I18nSettings = I18nSettings(locale = "en_AU", language = "en"),
            activated: Boolean = true,
        ) = jdbcAggregateTemplate.save(
            PlatformUser(
                userName = userName,
                passwordHash = passwordHash,
                isAdmin = isAdmin,
                documentsStorage = documentsStorage,
                i18nSettings = i18nSettings,
                activated = activated
            )
        )

        fun userActivationToken(
            user: PlatformUser? = null,
            token: String = RandomStringUtils.randomAscii(10),
            expiresAt: Instant = MOCK_TIME,
        ): UserActivationToken {
            val userId = if (user == null) platformUser().id else user.id
            return jdbcAggregateTemplate.save(
                UserActivationToken(
                    userId = userId!!,
                    token = token,
                    expiresAt = expiresAt,
                )
            )
        }
    }
}
