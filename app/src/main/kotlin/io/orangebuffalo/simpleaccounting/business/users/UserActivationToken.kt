package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * A token that can be used to activate a user account.
 *
 * Typically, created together with a new user account and shared with the user.
 * The user then uses the token to activate the account and set the password.
 */
@Table
data class UserActivationToken(
    /**
     * User this token is intended for. It is not possible to activate a user account with a token that is not
     * intended for this user.
     */

    val userId: String,
    /**
     * The token value. It is a essentially a random string.
     */
    val token: String,

    /**
     * The time when the token expires. After this time, the token is no longer valid.
     */
    val expiresAt: Instant,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()
