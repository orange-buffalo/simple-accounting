package io.orangebuffalo.simpleaccounting.business.api.auth

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthAuthenticationPurpose
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthAuthenticationRequest
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@DisplayName("startOAuthLogin mutation")
class StartOAuthLoginMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val nimbus = oauthProvider(
                name = "Nimbus Auth",
                clientId = "nimbus-client-id",
                authorizationUrl = "https://nimbus.example/authorize",
                scopes = setOf("openid", "email"),
            )
            val fry = fry().also {
                userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
            }
            val bender = bender()
            val scruffy = platformUser(userName = "Scruffy", activated = false).also {
                userOAuthIdentity(user = it, provider = nimbus, externalId = "scruffy-at-nimbus")
            }
        }
    }

    @Nested
    @DisplayName("Input Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputValidation {
        // valid boundary values are not applicable here: any non-blank username or provider id
        // that passes the constraints still has to resolve to a linked identity
        fun testCases() = listOf(
            mustNotBeBlankTestCases("userName") { value -> startOAuthLoginMutation(userName = value) },
            mustNotBeBlankTestCases("providerId") { value -> startOAuthLoginMutation(providerId = value) },
        ).flatten().filterNot { it is GraphqlMutationValidBoundaryTestCase }

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client.buildInputValidationRequest(testCase)
                .fromAnonymous()
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.StartOAuthLogin)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should build the authorization URL and remember the pending request`() {
            var authorizationUrl = ""
            client.graphqlMutation {
                startOAuthLoginMutation(
                    userName = "Fry",
                    providerId = preconditions.nimbus.id!!,
                    issueRefreshTokenCookie = true,
                )
            }
                .fromAnonymous()
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    authorizationUrl = Json.parseToJsonElement(this)
                        .jsonObject["data"]!!
                        .jsonObject[DgsConstants.MUTATION.StartOAuthLogin]!!
                        .jsonObject["authorizationUrl"]!!
                        .jsonPrimitive.content
                }

            val request = aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeSingle()
            request.userId.shouldBe(preconditions.fry.id)
            request.providerId.shouldBe(preconditions.nimbus.id)
            request.purpose.shouldBe(OAuthAuthenticationPurpose.LOGIN)
            request.issueRefreshTokenCookie.shouldBe(true)
            request.expiresAt.shouldBe(MOCK_TIME.plusSeconds(600))

            withHint("Should build a valid authorization code flow URL") {
                authorizationUrl.shouldStartWith("https://nimbus.example/authorize?")
                val queryParams = authorizationUrl.queryParams()
                queryParams["client_id"].shouldBe("nimbus-client-id")
                queryParams["response_type"].shouldBe("code")
                queryParams["state"].shouldBe(request.state)
                queryParams["redirect_uri"]
                    .shouldBe("${simpleAccountingProperties.publicUrl}/oauth-identity-callback")
                queryParams["scope"]?.split(" ").shouldContainExactlyInAnyOrder("openid", "email")
            }
        }

        @Test
        fun `should hand the browser a binding cookie for the started flow`() {
            client.graphqlMutation { startOAuthLoginMutation() }
                .fromAnonymous()
                .execute()
                .expectStatus().isOk
                .expectHeader().value(HttpHeaders.SET_COOKIE) { cookie ->
                    val request = aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeSingle()
                    withHint("The binding secret must never be part of the authorization request") {
                        cookie.shouldContain("$OAUTH_FLOW_BINDING_COOKIE=${request.browserBinding}")
                        request.browserBinding.shouldNotBe(request.state)
                    }
                    cookie.shouldContain("Max-Age=600")
                    cookie.shouldContain("Path=/api")
                    cookie.lowercase().shouldContain("httponly")
                    cookie.shouldContain("SameSite=Strict")
                }
        }

        @Test
        fun `should replace the previous pending request of the same browser`() {
            client.graphqlMutation { startOAuthLoginMutation() }
                .fromAnonymous()
                .execute()
                .expectStatus().isOk

            val firstBinding = aggregateTemplate.findAll<OAuthAuthenticationRequest>()
                .shouldBeSingle()
                .browserBinding

            client.graphqlMutation { startOAuthLoginMutation() }
                .fromAnonymous()
                .cookie(OAUTH_FLOW_BINDING_COOKIE, firstBinding)
                .execute()
                .expectStatus().isOk

            withHint("A browser retrying the login should not accumulate pending requests") {
                aggregateTemplate.findAll<OAuthAuthenticationRequest>()
                    .shouldBeSingle()
                    .browserBinding.shouldNotBe(firstBinding)
            }
        }

        @Test
        fun `should not invalidate a pending request of another browser`() {
            client.graphqlMutation { startOAuthLoginMutation() }
                .fromAnonymous()
                .execute()
                .expectStatus().isOk

            val legitimateRequest = aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeSingle()

            // an attacker knows the username and the provider id, but not the binding of the victim
            client.graphqlMutation { startOAuthLoginMutation() }
                .fromAnonymous()
                .cookie(OAUTH_FLOW_BINDING_COOKIE, "binding-of-the-attacker-browser")
                .execute()
                .expectStatus().isOk

            withHint("The login of the legitimate browser must remain completable") {
                aggregateTemplate.findAll<OAuthAuthenticationRequest>()
                    .map { it.state }
                    .shouldContain(legitimateRequest.state)
            }
        }

        @Test
        fun `should remove expired requests of other users when a new flow starts`() {
            preconditions {
                oauthAuthenticationRequest(
                    user = preconditions.bender,
                    provider = preconditions.nimbus,
                    state = "expired-state",
                    expiresAt = MOCK_TIME.minusSeconds(1),
                )
            }

            client.graphqlMutation { startOAuthLoginMutation() }
                .fromAnonymous()
                .execute()
                .expectStatus().isOk

            aggregateTemplate.findAll<OAuthAuthenticationRequest>()
                .shouldBeSingle()
                .userId.shouldBe(preconditions.fry.id)
        }

        @Test
        fun `should return USER_NOT_ACTIVATED error for not activated users`() {
            client.graphqlMutation {
                startOAuthLoginMutation(userName = "Scruffy", providerId = preconditions.nimbus.id!!)
            }
                .fromAnonymous()
                .executeAndVerifyBusinessError(
                    message = "User is not activated",
                    errorCode = "USER_NOT_ACTIVATED",
                    path = DgsConstants.MUTATION.StartOAuthLogin,
                )

            aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeEmpty()
        }

        @Test
        fun `should return LOGIN_NOT_AVAILABLE error when the user has no identity at the provider`() {
            client.graphqlMutation {
                startOAuthLoginMutation(userName = "Bender", providerId = preconditions.nimbus.id!!)
            }
                .fromAnonymous()
                .executeAndVerifyBusinessError(
                    message = "User 'Bender' has no identity linked at provider ${preconditions.nimbus.id}",
                    errorCode = "LOGIN_NOT_AVAILABLE",
                    path = DgsConstants.MUTATION.StartOAuthLogin,
                )

            aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeEmpty()
        }

        @Test
        fun `should return LOGIN_NOT_AVAILABLE error for unknown user`() {
            client.graphqlMutation {
                startOAuthLoginMutation(userName = "Lrrr", providerId = preconditions.nimbus.id!!)
            }
                .fromAnonymous()
                .executeAndVerifyBusinessError(
                    message = "User 'Lrrr' has no identity linked at provider ${preconditions.nimbus.id}",
                    errorCode = "LOGIN_NOT_AVAILABLE",
                    path = DgsConstants.MUTATION.StartOAuthLogin,
                )
        }
    }

    private fun String.queryParams(): Map<String, String> = URI(this).rawQuery
        .split("&")
        .associate { param ->
            val (name, value) = param.split("=", limit = 2)
            URLDecoder.decode(name, StandardCharsets.UTF_8) to URLDecoder.decode(value, StandardCharsets.UTF_8)
        }

    private fun MutationProjection.startOAuthLoginMutation(
        userName: String = "Fry",
        providerId: String = preconditions.nimbus.id!!,
        issueRefreshTokenCookie: Boolean? = null,
    ) = startOAuthLogin(
        userName = userName,
        providerId = providerId,
        issueRefreshTokenCookie = issueRefreshTokenCookie,
    ) {
        authorizationUrl
    }
}
