package io.orangebuffalo.simpleaccounting.business.oauthproviders

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * A link between a platform user and their identity at one of the registered [OAuthProvider]s.
 *
 * As soon as a user has at least one identity linked, password login is no longer available for them.
 */
@Table("USER_OAUTH_IDENTITY")
data class UserOAuthIdentity(
    val userId: String,
    val providerId: String,

    /**
     * Value of the [OAuthProvider.userIdAttribute] as reported by the provider for this user.
     */
    val externalId: String,

    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()
