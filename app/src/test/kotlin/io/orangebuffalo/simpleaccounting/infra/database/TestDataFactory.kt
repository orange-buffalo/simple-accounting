package io.orangebuffalo.simpleaccounting.infra.database

import io.orangebuffalo.simpleaccounting.domain.users.I18nSettings
import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import io.orangebuffalo.simpleaccounting.domain.users.UserActivationToken
import io.orangebuffalo.simpleaccounting.infra.database.TestDataFactory.EntitiesFactory
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

/**
 * Factory for creating test data (preconditions) in the database.
 * In fact, it is a synthetic sugar on top of [EntitiesFactory] in order to allow for
 * anonymous inline class instances creation via `object` keyword. This allows to define the preconditions
 * without the excessive boilerplate of dedicated classes.
 *
 * The invocation of the factory is the same as calling [TestDataFactory.setupAndCommit] and
 * can be used for more concise test setup:
 *
 * ```
 * fun `test method`(testDataFactory: TestDataFactory) {
 *     val preconditions = testDataFactory {
 *         object {
 *             val entity = <any EntitiesFactory method call>
 *             ...
 *         }
 *     }
 *     ...
 *     preconditions.entity // this will return the entity created by the factory
 *     ...
 * }
 * ```
 *
 * To reuse preconditions across multiple test methods, one could create an extension function:
 * ```
 * fun TestDataFactory.preconditions() = setupAndCommit {
 *    ...
 * }
 *
 * fun `test method`(testDataFactory: TestDataFactory) {
 *    val preconditions = testDataFactory.preconditions()
 *    ...
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

    /**
     * Same as [setupAndCommit] but with a more concise syntax.
     */
    operator fun <T> invoke(spec: EntitiesFactory.() -> T): T = setupAndCommit(spec)

    /**
     * Creates the entities as per [spec] and commits the transaction.
     */
    fun <T> setupAndCommit(spec: EntitiesFactory.() -> T): T = transactionTemplate.execute {
        spec(EntitiesFactory())
    }!!

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
