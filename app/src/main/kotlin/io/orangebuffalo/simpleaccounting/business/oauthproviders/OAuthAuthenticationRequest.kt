package io.orangebuffalo.simpleaccounting.business.oauthproviders

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * An OAuth2 authorization code flow that has been initiated by the application and is pending
 * the callback from the authorization server.
 *
 * Completing the flow requires both the [state] returned by the authorization server and the
 * [browserBinding] held by the browser, and consumes the request.
 */
@Table("OAUTH_AUTHENTICATION_REQUEST")
data class OAuthAuthenticationRequest(
    val state: String,

    /**
     * Secret shared with the browser that started the flow via an HTTP only cookie, and never
     * disclosed to the authorization server. Completing the flow requires presenting it back,
     * so that a party that only observed [state] cannot finish the flow.
     */
    val browserBinding: String,

    val providerId: String,

    /**
     * User this flow is executed for: the one being logged in for [OAuthAuthenticationPurpose.LOGIN],
     * the currently authenticated one for [OAuthAuthenticationPurpose.LINK].
     */
    val userId: String,

    val purpose: OAuthAuthenticationPurpose,

    /**
     * Whether a refresh token cookie should be issued once the login flow completes.
     * Only meaningful for [OAuthAuthenticationPurpose.LOGIN].
     */
    val issueRefreshTokenCookie: Boolean,

    val expiresAt: Instant,

    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()

enum class OAuthAuthenticationPurpose {
    /**
     * The user is authenticating into the application with a previously linked identity.
     */
    LOGIN,

    /**
     * An authenticated user is linking a new identity to their profile.
     */
    LINK,
}
