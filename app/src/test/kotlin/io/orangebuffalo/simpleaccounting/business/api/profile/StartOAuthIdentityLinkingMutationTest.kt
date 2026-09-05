package io.orangebuffalo.simpleaccounting.business.api.profile

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
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

@DisplayName("startOAuthIdentityLinking mutation")
class StartOAuthIdentityLinkingMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val nimbus = oauthProvider(
                name = "Nimbus Auth",
                clientId = "nimbus-client-id",
                authorizationUrl = "https://nimbus.example/authorize",
            )
            val fry = fry()
            val bender = bender().also {
                userOAuthIdentity(user = it, provider = nimbus, externalId = "bender-at-nimbus")
            }
            val fryWorkspaceAccessToken = workspaceAccessToken(
                workspace = workspace(owner = fry),
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphqlMutation { startOAuthIdentityLinkingMutation() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.StartOAuthIdentityLinking)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client.graphqlMutation { startOAuthIdentityLinkingMutation() }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.StartOAuthIdentityLinking)
        }
    }

    @Nested
    @DisplayName("Input Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputValidation {
        // valid boundary values are not applicable here: any non-blank provider id
        // still has to resolve to a registered provider
        fun testCases() = mustNotBeBlankTestCases("providerId") { value ->
            startOAuthIdentityLinkingMutation(providerId = value)
        }.filterNot { it is GraphqlMutationValidBoundaryTestCase }

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client.buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.StartOAuthIdentityLinking)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should build the authorization URL and remember the pending request`() {
            var authorizationUrl = ""
            client.graphqlMutation { startOAuthIdentityLinkingMutation() }
                .from(preconditions.fry)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    authorizationUrl = Json.parseToJsonElement(this)
                        .jsonObject["data"]!!
                        .jsonObject[DgsConstants.MUTATION.StartOAuthIdentityLinking]!!
                        .jsonObject["authorizationUrl"]!!
                        .jsonPrimitive.content
                }

            authorizationUrl.shouldStartWith("https://nimbus.example/authorize?")

            val request = aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeSingle()
            request.userId.shouldBe(preconditions.fry.id)
            request.providerId.shouldBe(preconditions.nimbus.id)
            request.purpose.shouldBe(OAuthAuthenticationPurpose.LINK)
            request.issueRefreshTokenCookie.shouldBe(false)
        }

        @Test
        fun `should return PROVIDER_ALREADY_LINKED error when an identity is already linked`() {
            client.graphqlMutation { startOAuthIdentityLinkingMutation() }
                .from(preconditions.bender)
                .executeAndVerifyBusinessError(
                    message = "An identity at provider ${preconditions.nimbus.id} is already linked to this user",
                    errorCode = "PROVIDER_ALREADY_LINKED",
                    path = DgsConstants.MUTATION.StartOAuthIdentityLinking,
                )

            aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeEmpty()
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error for unknown provider`() {
            client.graphqlMutation { startOAuthIdentityLinkingMutation(providerId = "unknown-id") }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.StartOAuthIdentityLinking)
        }
    }

    private fun MutationProjection.startOAuthIdentityLinkingMutation(
        providerId: String = preconditions.nimbus.id!!,
    ) = startOAuthIdentityLinking(providerId = providerId) {
        authorizationUrl
    }
}
