package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProvider
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val USER_AUTH_ISSUER_ID = "user-auth"
private const val USER_AUTH_CLIENT_ID = "test-user-auth-client-id"
private const val USER_AUTH_CLIENT_SECRET = "test-user-auth-client-secret"

// spring oauth has clock skew of 1 minute, so expiration must be greater than that
private const val USER_AUTH_TOKEN_EXPIRY_SECONDS = 300L

/**
 * Mocks an OAuth2 provider that admins can register for authenticating the users of the application.
 * As opposed to [GoogleOAuthMocks], this provider is not known to the application configuration:
 * it is created as a regular entity in the test preconditions, pointing to these endpoints.
 */
object UserAuthOAuthMocks : OAuthMocksProvider by OAuthMocks.provider(
    issuerId = USER_AUTH_ISSUER_ID,
    clientId = USER_AUTH_CLIENT_ID,
    clientSecret = USER_AUTH_CLIENT_SECRET,
    scopes = listOf("openid"),
    clientRegistrationId = USER_AUTH_ISSUER_ID,
) {
    const val CLIENT_ID = USER_AUTH_CLIENT_ID
    const val CLIENT_SECRET = USER_AUTH_CLIENT_SECRET

    val authorizationUrl: String
        get() = OAuthMocks.mockOAuthServer.authorizationEndpointUrl(USER_AUTH_ISSUER_ID).toString()

    val tokenUrl: String
        get() = OAuthMocks.mockOAuthServer.tokenEndpointUrl(USER_AUTH_ISSUER_ID).toString()

    val userInfoUrl: String
        get() = OAuthMocks.mockOAuthServer.userInfoUrl(USER_AUTH_ISSUER_ID).toString()

    /**
     * An endpoint of the mock server that does not exist, to emulate a provider that is
     * misconfigured or unavailable.
     */
    val unavailableEndpointUrl: String
        get() = OAuthMocks.mockOAuthServer.issuerUrl(USER_AUTH_ISSUER_ID).toString() + "/not-an-endpoint"

    /**
     * Performs the browser part of the authorization code flow and returns the code the
     * authorization server has issued. The code is single use and is only valid for the
     * provided redirect URI.
     */
    fun issueAuthorizationCode(redirectUri: String): String {
        val authorizationRequestUri = URI.create(
            "$authorizationUrl?response_type=code&client_id=$USER_AUTH_CLIENT_ID" +
                    "&scope=openid&state=test-state&redirect_uri=${redirectUri.urlEncoded()}"
        )
        val response = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()
            .send(
                HttpRequest.newBuilder(authorizationRequestUri).GET().build(),
                HttpResponse.BodyHandlers.discarding(),
            )
        withHint("Authorization endpoint should redirect back with the code") {
            response.statusCode().shouldBe(302)
        }
        val location = response.headers().firstValue("location").orElse(null).shouldNotBeNull()
        return URI.create(location).query
            .split("&")
            .firstOrNull { it.startsWith("code=") }
            ?.removePrefix("code=")
            .shouldNotBeNull()
    }

    /**
     * Sets up the provider to report the identity with the provided ID for the next token exchange.
     */
    fun mockIdentity(externalId: String, claims: Map<String, Any> = emptyMap()) {
        OAuthMocks.resetCurrentTokensQueue()
        OAuthMocks.mockOAuthServer.enqueueCallback(
            DefaultOAuth2TokenCallback(
                issuerId = USER_AUTH_ISSUER_ID,
                subject = externalId,
                audience = listOf(USER_AUTH_CLIENT_ID),
                claims = claims,
                expiry = USER_AUTH_TOKEN_EXPIRY_SECONDS,
            )
        )
    }
}

private fun String.urlEncoded() = URLEncoder.encode(this, StandardCharsets.UTF_8)

/**
 * Registers a provider that points to [UserAuthOAuthMocks] endpoints, as an admin would do
 * for a real authorization server.
 */
fun EntitiesFactory.mockedOAuthProvider(
    name: String = "Nimbus Auth",
    userIdAttribute: String = "sub",
    tokenUrl: String = UserAuthOAuthMocks.tokenUrl,
    userInfoUrl: String = UserAuthOAuthMocks.userInfoUrl,
): OAuthProvider = oauthProvider(
    name = name,
    clientId = UserAuthOAuthMocks.CLIENT_ID,
    clientSecret = UserAuthOAuthMocks.CLIENT_SECRET,
    authorizationUrl = UserAuthOAuthMocks.authorizationUrl,
    tokenUrl = tokenUrl,
    userInfoUrl = userInfoUrl,
    userIdAttribute = userIdAttribute,
    scopes = setOf("openid"),
)
