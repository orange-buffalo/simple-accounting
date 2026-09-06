package io.orangebuffalo.simpleaccounting.business.api.auth

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAUTH_IDENTITY_CALLBACK_PATH
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthAuthenticationPurpose
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthAuthenticationRequest
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProvider
import io.orangebuffalo.simpleaccounting.business.oauthproviders.UserOAuthIdentity
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshToken
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.GraphqlClientRequestExecutor
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.UserAuthOAuthMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.mockedOAuthProvider
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

private const val BROWSER_BINDING = "test-browser-binding"

@DisplayName("completeOAuthAuthentication mutation")
class CompleteOAuthAuthenticationMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Login flow")
    inner class LoginFlow {

        @Test
        fun `should issue an access token when the linked identity is authenticated`() {
            val testData = preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                        loginRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

            completeFlow()
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CompleteOAuthAuthentication to buildJsonObject {
                        put("outcome", "LOGIN")
                        put("accessToken", JsonValues.ANY_STRING)
                    }
                )

            aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeEmpty()
            aggregateTemplate.findAll<RefreshToken>().shouldBeEmpty()
            aggregateTemplate.findAll<UserOAuthIdentity>()
                .shouldBeSingle()
                .userId.shouldBe(testData.fry.id)
        }

        @Test
        fun `should issue a refresh token cookie when requested on login start`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                        loginRequest(user = it, provider = provider, issueRefreshTokenCookie = true)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

            completeFlow()
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CompleteOAuthAuthentication to buildJsonObject {
                        put("outcome", "LOGIN")
                        put("accessToken", JsonValues.ANY_STRING)
                    }
                )

            aggregateTemplate.findAll<RefreshToken>().shouldBeSingle()
        }

        @Test
        fun `should authenticate by the user id attribute configured for the provider`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider(userIdAttribute = "email")
                    val fry = fry().also {
                        userOAuthIdentity(
                            user = it,
                            provider = provider,
                            externalId = "fry@planet-express.example",
                        )
                        loginRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity(
                externalId = "some-opaque-subject",
                claims = mapOf("email" to "fry@planet-express.example"),
            )

            completeFlow()
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CompleteOAuthAuthentication to buildJsonObject {
                        put("outcome", "LOGIN")
                        put("accessToken", JsonValues.ANY_STRING)
                    }
                )
        }

        @Test
        fun `should return IDENTITY_MISMATCH error when another identity is authenticated`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                        loginRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("bender-at-nimbus")

            completeFlow()
                .executeAndVerifyBusinessError(
                    message = "The authenticated identity does not match the identity linked to this user",
                    errorCode = "IDENTITY_MISMATCH",
                    path = DgsConstants.MUTATION.CompleteOAuthAuthentication,
                )
        }

        @Test
        fun `should return IDENTITY_MISMATCH error when the identity was unlinked after the flow started`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        loginRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

            completeFlow()
                .executeAndVerifyBusinessError(
                    message = "The authenticated identity does not match the identity linked to this user",
                    errorCode = "IDENTITY_MISMATCH",
                    path = DgsConstants.MUTATION.CompleteOAuthAuthentication,
                )

            aggregateTemplate.findAll<RefreshToken>().shouldBeEmpty()
        }

        @Test
        fun `should return USER_NOT_ACTIVATED error when the account was deactivated after the flow started`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val scruffy = platformUser(userName = "Scruffy", activated = false).also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "scruffy-at-nimbus")
                        loginRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("scruffy-at-nimbus")

            completeFlow()
                .executeAndVerifyBusinessError(
                    message = "User is not activated",
                    errorCode = "USER_NOT_ACTIVATED",
                    path = DgsConstants.MUTATION.CompleteOAuthAuthentication,
                )

            aggregateTemplate.findAll<RefreshToken>().shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("Linking flow")
    inner class LinkingFlow {

        @Test
        fun `should link the authenticated identity without granting a session`() {
            val testData = preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        linkRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

            completeFlow()
                .executeAndVerifyResponse(
                    DgsConstants.MUTATION.CompleteOAuthAuthentication to buildJsonObject {
                        put("outcome", "LINK")
                        put("accessToken", JsonNull)
                    }
                )

            val identity = aggregateTemplate.findAll<UserOAuthIdentity>().shouldBeSingle()
            identity.userId.shouldBe(testData.fry.id)
            identity.providerId.shouldBe(testData.provider.id)
            identity.externalId.shouldBe("fry-at-nimbus")
            aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeEmpty()
            aggregateTemplate.findAll<RefreshToken>().shouldBeEmpty()
        }

        @Test
        fun `should return IDENTITY_ALREADY_IN_USE error when the identity belongs to another user`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val bender = bender().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "shared-identity")
                    }
                    val fry = fry().also {
                        linkRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("shared-identity")

            completeFlow()
                .executeAndVerifyBusinessError(
                    message = "The authenticated identity is already linked to another user",
                    errorCode = "IDENTITY_ALREADY_IN_USE",
                    path = DgsConstants.MUTATION.CompleteOAuthAuthentication,
                )

            aggregateTemplate.findAll<UserOAuthIdentity>().shouldBeSingle()
        }

        @Test
        fun `should return PROVIDER_ALREADY_LINKED error when an identity was linked after the flow started`() {
            val testData = preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        linkRequest(user = it, provider = provider)
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("another-fry-at-nimbus")

            completeFlow()
                .executeAndVerifyBusinessError(
                    message = "An identity at provider ${testData.provider.id} is already linked to this user",
                    errorCode = "PROVIDER_ALREADY_LINKED",
                    path = DgsConstants.MUTATION.CompleteOAuthAuthentication,
                )

            aggregateTemplate.findAll<UserOAuthIdentity>()
                .shouldBeSingle()
                .externalId.shouldBe("fry-at-nimbus")
        }
    }

    @Nested
    @DisplayName("Authorization request validation")
    inner class AuthorizationRequestValidation {

        @Test
        fun `should return UNKNOWN_AUTHORIZATION_REQUEST error for unknown state`() {
            preconditions {
                object {
                    val fry = fry()
                }
            }

            completeFlow(state = "never-issued-state", code = "unused-code")
                .executeAndVerifyUnknownRequest()
        }

        @Test
        fun `should return UNKNOWN_AUTHORIZATION_REQUEST error for expired state and remove the request`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        loginRequest(user = it, provider = provider, expiresAt = MOCK_TIME.minusSeconds(1))
                    }
                }
            }

            completeFlow(code = "unused-code").executeAndVerifyUnknownRequest()

            aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeEmpty()
        }

        @Test
        fun `should return UNKNOWN_AUTHORIZATION_REQUEST error for state expiring exactly now`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        loginRequest(user = it, provider = provider, expiresAt = MOCK_TIME)
                    }
                }
            }

            completeFlow(code = "unused-code").executeAndVerifyUnknownRequest()
        }

        @Test
        fun `should not allow the same state to be used twice`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                        loginRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

            completeFlow()
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CompleteOAuthAuthentication to buildJsonObject {
                        put("outcome", "LOGIN")
                        put("accessToken", JsonValues.ANY_STRING)
                    }
                )

            UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")
            completeFlow().executeAndVerifyUnknownRequest()

            aggregateTemplate.findAll<RefreshToken>().shouldBeEmpty()
        }

        @Test
        fun `should return UNKNOWN_AUTHORIZATION_REQUEST error when the browser binding is not presented`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                        loginRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

            client.graphqlMutation { completeOAuthAuthenticationMutation(code = "unused-code") }
                .fromAnonymous()
                .executeAndVerifyUnknownRequest()

            withHintOnPendingRequest()
        }

        @Test
        fun `should return UNKNOWN_AUTHORIZATION_REQUEST error when another browser binding is presented`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                        loginRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

            completeFlow(code = "unused-code", browserBinding = "binding-of-another-browser")
                .executeAndVerifyUnknownRequest()

            withHintOnPendingRequest()
        }

        @Test
        fun `should return AUTHORIZATION_FAILED error when the provider reports an error`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                        loginRequest(user = it, provider = provider)
                    }
                }
            }

            client.graphqlMutation {
                completeOAuthAuthentication(code = null, error = "access_denied", state = "test-state") {
                    outcome
                    accessToken
                }
            }
                .fromAnonymous()
                .cookie(OAUTH_FLOW_BINDING_COOKIE, BROWSER_BINDING)
                .executeAndVerifyBusinessError(
                    message = "Authorization has not been granted: access_denied",
                    errorCode = "AUTHORIZATION_FAILED",
                    path = DgsConstants.MUTATION.CompleteOAuthAuthentication,
                )

            aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeEmpty()
        }

        @Test
        fun `should return AUTHORIZATION_FAILED error when neither code nor error is provided`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider()
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                        loginRequest(user = it, provider = provider)
                    }
                }
            }

            client.graphqlMutation {
                completeOAuthAuthentication(code = null, error = null, state = "test-state") {
                    outcome
                    accessToken
                }
            }
                .fromAnonymous()
                .cookie(OAUTH_FLOW_BINDING_COOKIE, BROWSER_BINDING)
                .executeAndVerifyBusinessError(
                    message = "Authorization has not been granted: authorization code is not provided",
                    errorCode = "AUTHORIZATION_FAILED",
                    path = DgsConstants.MUTATION.CompleteOAuthAuthentication,
                )
        }
    }

    @Nested
    @DisplayName("Provider failures")
    inner class ProviderFailures {

        @Test
        fun `should return AUTHORIZATION_FAILED error when the token endpoint is not available`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider(tokenUrl = UserAuthOAuthMocks.unavailableEndpointUrl)
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                        loginRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

            completeFlow()
                .executeAndVerifyBusinessError(
                    message = "Authorization has not been granted: token endpoint request failed",
                    errorCode = "AUTHORIZATION_FAILED",
                    path = DgsConstants.MUTATION.CompleteOAuthAuthentication,
                )

            aggregateTemplate.findAll<RefreshToken>().shouldBeEmpty()
        }

        @Test
        fun `should return AUTHORIZATION_FAILED error when the user info endpoint is not available`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider(userInfoUrl = UserAuthOAuthMocks.unavailableEndpointUrl)
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                        loginRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

            completeFlow()
                .executeAndVerifyBusinessError(
                    message = "Authorization has not been granted: user info request failed",
                    errorCode = "AUTHORIZATION_FAILED",
                    path = DgsConstants.MUTATION.CompleteOAuthAuthentication,
                )
        }

        @Test
        fun `should return AUTHORIZATION_FAILED error when the user info has no configured attribute`() {
            preconditions {
                object {
                    val provider = mockedOAuthProvider(userIdAttribute = "custom-identity-claim")
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = provider, externalId = "fry-at-nimbus")
                        loginRequest(user = it, provider = provider)
                    }
                }
            }
            UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

            completeFlow()
                .executeAndVerifyBusinessError(
                    message = "Authorization has not been granted: " +
                            "user info has no 'custom-identity-claim' attribute",
                    errorCode = "AUTHORIZATION_FAILED",
                    path = DgsConstants.MUTATION.CompleteOAuthAuthentication,
                )
        }
    }

    private fun withHintOnPendingRequest() = withHint(
        "The pending request should survive a callback that cannot prove it belongs to this browser"
    ) {
        aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeSingle()
    }

    private fun GraphqlClientRequestExecutor.executeAndVerifyUnknownRequest() = executeAndVerifyBusinessError(
        message = "Authorization request is not known or has expired",
        errorCode = "UNKNOWN_AUTHORIZATION_REQUEST",
        path = DgsConstants.MUTATION.CompleteOAuthAuthentication,
    )

    /**
     * Emulates the browser part of the flow: the user grants the access at the authorization
     * server, which issues a single use code for our redirect URI, and the browser presents the
     * binding cookie it received when the flow was started.
     */
    private fun completeFlow(
        state: String = "test-state",
        code: String = UserAuthOAuthMocks.issueAuthorizationCode(
            "${simpleAccountingProperties.publicUrl}$OAUTH_IDENTITY_CALLBACK_PATH"
        ),
        browserBinding: String = BROWSER_BINDING,
    ): GraphqlClientRequestExecutor = client
        .graphqlMutation { completeOAuthAuthenticationMutation(state = state, code = code) }
        .fromAnonymous()
        .cookie(OAUTH_FLOW_BINDING_COOKIE, browserBinding)

    private fun MutationProjection.completeOAuthAuthenticationMutation(
        code: String,
        state: String = "test-state",
    ) = completeOAuthAuthentication(code = code, error = null, state = state) {
        outcome
        accessToken
    }
}

private fun EntitiesFactory.loginRequest(
    user: PlatformUser,
    provider: OAuthProvider,
    issueRefreshTokenCookie: Boolean = false,
    expiresAt: Instant = MOCK_TIME.plusSeconds(600),
) = oauthAuthenticationRequest(
    user = user,
    provider = provider,
    state = "test-state",
    browserBinding = BROWSER_BINDING,
    purpose = OAuthAuthenticationPurpose.LOGIN,
    issueRefreshTokenCookie = issueRefreshTokenCookie,
    expiresAt = expiresAt,
)

private fun EntitiesFactory.linkRequest(
    user: PlatformUser,
    provider: OAuthProvider,
) = oauthAuthenticationRequest(
    user = user,
    provider = provider,
    state = "test-state",
    browserBinding = BROWSER_BINDING,
    purpose = OAuthAuthenticationPurpose.LINK,
)
