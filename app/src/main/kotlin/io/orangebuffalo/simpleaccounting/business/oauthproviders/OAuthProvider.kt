package io.orangebuffalo.simpleaccounting.business.oauthproviders

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * An OAuth2 authorization server that users can authenticate with.
 *
 * The application ships without any predefined providers: they are registered by admins,
 * who provide the client credentials issued by the provider and the endpoints to use.
 */
@Table("OAUTH_PROVIDER")
data class OAuthProvider(
    /**
     * Human-readable name of the provider, as presented to the users on the login page.
     */
    val name: String,

    /**
     * Client ID issued by the provider for this application.
     */
    val clientId: String,

    /**
     * Client secret issued by the provider for this application.
     */
    val clientSecret: String,

    /**
     * Endpoint the users are redirected to in order to grant access to their identity.
     */
    val authorizationUrl: String,

    /**
     * Endpoint used to exchange the authorization code for an access token.
     */
    val tokenUrl: String,

    /**
     * Endpoint used to retrieve the details of the authenticated identity.
     */
    val userInfoUrl: String,

    /**
     * Name of the attribute in the user info response that uniquely identifies the user
     * within the provider, e.g. `sub` or `email`.
     */
    val userIdAttribute: String,

    @field:MappedCollection(idColumn = "PROVIDER_ID")
    val scopes: Set<OAuthProviderScope>,

    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()

@Table("OAUTH_PROVIDER_SCOPE")
data class OAuthProviderScope(
    val scope: String,
)
