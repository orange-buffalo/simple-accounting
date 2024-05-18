package io.orangebuffalo.simpleaccounting.infra.database

import io.orangebuffalo.simpleaccounting.domain.documents.Document
import io.orangebuffalo.simpleaccounting.domain.users.I18nSettings
import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import io.orangebuffalo.simpleaccounting.domain.users.UserActivationToken
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

/**
 * API for creating preconditions in the database.
 *
 * The API will create any missing dependencies automatically. Do not rely on the default
 * values in this class and always override all properties that influence test behavior.
 *
 * Important! Invoking any methods of this class immediately saves the created entity to the database,
 * hence it must be called from within the test method execution, otherwise the saved entities
 * will be cleaned up by the test framework before the test method is executed.
 *
 * To use:
 * 1. Inject the [PreconditionsInfra] into your test constructor: `@Autowired private val preconditionsInfra: PreconditionsInfra`
 * 2. Create an object of [Preconditions] in your test method or a factory method:
 * ```
 * val preconditions = object: Preconditions(preconditionsInfra) {
 *   val myEntity = <entity factory method>(<entity properties>)
 *   ...
 * }
 * ```
 * 3. Use the created entities in the test code as needed: `preconditions.myEntity.<entity property>`.
 */
abstract class Preconditions(private val infra: PreconditionsInfra) {
    fun platformUser(
        userName: String = "Farnsworth",
        passwordHash: String = "nopassword",
        isAdmin: Boolean = false,
        documentsStorage: String? = null,
        i18nSettings: I18nSettings = I18nSettings(locale = "en_AU", language = "en"),
        activated: Boolean = true,
    ) = save(
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
        token: String = RandomStringUtils.randomAlphabetic(10),
        expiresAt: Instant = MOCK_TIME,
    ): UserActivationToken {
        val userId = if (user == null) platformUser().id else user.id
        return save(
            UserActivationToken(
                userId = userId!!,
                token = token,
                expiresAt = expiresAt,
            )
        )
    }

    /**
     * Regular user
     */
    fun fry() = platformUser(
        userName = "Fry",
        passwordHash = "qwertyHash",
        isAdmin = false
    )

    /**
     * Admin user, not workspaces expected to be mapped
     */
    fun farnsworth() = platformUser(
        userName = "Farnsworth",
        passwordHash = "scienceBasedHash",
        isAdmin = true,
    )

    fun zoidberg() = platformUser(
            userName = "Zoidberg",
            passwordHash = "??",
            isAdmin = false
        )

        fun roberto() = platformUser(
            userName = "Roberto",
            passwordHash = "o_O",
            isAdmin = false
        )

        fun mafiaBot() = platformUser(
            userName = "MafiaBot",
            passwordHash = "$$$",
            isAdmin = false
        )

        fun bender() = platformUser(
            userName = "Bender",
            passwordHash = "011101010101101001",
            isAdmin = false
        )

    fun workspace(
        name: String = "Planet Express",
        owner: PlatformUser? = null,
        taxEnabled: Boolean = true,
        multiCurrencyEnabled: Boolean = true,
        defaultCurrency: String = "USD"
    ): Workspace {
        val ownerId = if (owner == null) platformUser().id else owner.id
        return save(
            Workspace(
                name = name,
                ownerId = ownerId!!,
                taxEnabled = taxEnabled,
                multiCurrencyEnabled = multiCurrencyEnabled,
                defaultCurrency = defaultCurrency
            )
        )
    }

    fun workspaceAccessToken(
        workspace: Workspace? = null,
        timeCreated: Instant = MOCK_TIME,
        validTill: Instant = MOCK_TIME,
        revoked: Boolean = false,
        token: String = "token"
    ): WorkspaceAccessToken {
        val workspaceId = if (workspace == null) workspace().id else workspace.id
        return save(
            WorkspaceAccessToken(
                workspaceId = workspaceId!!,
                timeCreated = timeCreated,
                validTill = validTill,
                revoked = revoked,
                token = token
            )
        )
    }

    fun document(
        name: String = "Slurm Receipt",
        workspace: Workspace? = null,
        timeUploaded: Instant = MOCK_TIME,
        storageId: String = "test-storage",
        storageLocation: String? = "test-location",
        sizeInBytes: Long? = null
    ): Document {
        val workspaceId = if (workspace == null) workspace().id else workspace.id
        return save(
            Document(
                name = name,
                workspaceId = workspaceId!!,
                storageId = storageId,
                storageLocation = storageLocation,
                timeUploaded = timeUploaded,
                sizeInBytes = sizeInBytes
            )
        )
    }

    protected fun <T : Any> save(entity: T): T = infra.save(entity)
}

/**
 * Infrastructure for creating preconditions in the database.
 */
class PreconditionsInfra(
    platformTransactionManager: PlatformTransactionManager,
    private val jdbcAggregateTemplate: JdbcAggregateTemplate,
) {

    private val transactionTemplate = TransactionTemplate().apply {
        transactionManager = platformTransactionManager
        propagationBehavior = TransactionTemplate.PROPAGATION_REQUIRES_NEW
    }

    fun <T : Any> save(entity: T): T {
        return transactionTemplate.execute {
            jdbcAggregateTemplate.save(entity)
        }!!
    }
}
