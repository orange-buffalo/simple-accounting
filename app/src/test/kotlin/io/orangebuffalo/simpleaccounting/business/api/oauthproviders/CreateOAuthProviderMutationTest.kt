package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProvider
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("createOAuthProvider mutation")
class CreateOAuthProviderMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val farnsworth = farnsworth()
            val fry = fry()
            val fryWorkspaceAccessToken = workspaceAccessToken(
                workspace = workspace(owner = fry),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphqlMutation { createOAuthProviderMutation() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateOAuthProvider)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for regular user`() {
            client.graphqlMutation { createOAuthProviderMutation() }
                .from(preconditions.fry)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateOAuthProvider)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client.graphqlMutation { createOAuthProviderMutation() }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateOAuthProvider)
        }
    }

    @Nested
    @DisplayName("Input Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("name") { value -> createOAuthProviderMutation(name = value) },
            sizeConstraintTestCases("name", maxLength = 255) { value -> createOAuthProviderMutation(name = value) },
            mustNotBeBlankTestCases("clientId") { value -> createOAuthProviderMutation(clientId = value) },
            sizeConstraintTestCases("clientId", maxLength = 255) { value ->
                createOAuthProviderMutation(clientId = value)
            },
            mustNotBeBlankTestCases("clientSecret") { value -> createOAuthProviderMutation(clientSecret = value) },
            mustNotBeBlankTestCases("userIdAttribute") { value ->
                createOAuthProviderMutation(userIdAttribute = value)
            },
            // the endpoints are URLs, so the generic length boundaries do not apply to them
            endpointUrlFieldTestCases("authorizationUrl") { value ->
                createOAuthProviderMutation(authorizationUrl = value)
            },
            endpointUrlFieldTestCases("tokenUrl") { value -> createOAuthProviderMutation(tokenUrl = value) },
            endpointUrlFieldTestCases("userInfoUrl") { value -> createOAuthProviderMutation(userInfoUrl = value) },
        ).flatten()

        private fun endpointUrlFieldTestCases(
            fieldName: String,
            mutationWithFieldValue: MutationProjection.(fieldValue: String) -> MutationProjection,
        ) = mustNotBeBlankTestCases(fieldName, mutationWithFieldValue = mutationWithFieldValue)
            .filterNot { it is GraphqlMutationValidBoundaryTestCase } +
                endpointUrlTestCases(fieldName, maxLength = 2048, mutationWithFieldValue = mutationWithFieldValue)

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client.buildInputValidationRequest(testCase)
                .from(preconditions.farnsworth)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateOAuthProvider)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should register a new provider`() {
            client.graphqlMutation {
                createOAuthProviderMutation(
                    name = "Nimbus Auth",
                    clientId = "nimbus-client-id",
                    clientSecret = "nimbus-client-secret",
                    authorizationUrl = "https://nimbus.example/authorize",
                    tokenUrl = "https://nimbus.example/token",
                    userInfoUrl = "https://nimbus.example/userinfo",
                    userIdAttribute = "email",
                    scopes = listOf("openid", "email"),
                )
            }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateOAuthProvider to buildJsonObject {
                        put("id", JsonValues.ANY_STRING)
                        put("name", "Nimbus Auth")
                        put("clientId", "nimbus-client-id")
                        put("authorizationUrl", "https://nimbus.example/authorize")
                        put("tokenUrl", "https://nimbus.example/token")
                        put("userInfoUrl", "https://nimbus.example/userinfo")
                        put("userIdAttribute", "email")
                        putJsonArray("scopes") {
                            add(JsonPrimitive("email"))
                            add(JsonPrimitive("openid"))
                        }
                    }
                )

            val provider = aggregateTemplate.findAll<OAuthProvider>().shouldBeSingle()
            provider.name.shouldBe("Nimbus Auth")
            provider.clientId.shouldBe("nimbus-client-id")
            provider.clientSecret.shouldBe("nimbus-client-secret")
            provider.authorizationUrl.shouldBe("https://nimbus.example/authorize")
            provider.tokenUrl.shouldBe("https://nimbus.example/token")
            provider.userInfoUrl.shouldBe("https://nimbus.example/userinfo")
            provider.userIdAttribute.shouldBe("email")
            provider.scopes.map { it.scope }.shouldContainExactlyInAnyOrder("openid", "email")
        }

        @Test
        fun `should register a provider without scopes`() {
            client.graphqlMutation {
                createOAuthProviderMutation(name = "MomCorp ID", scopes = emptyList())
            }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateOAuthProvider to buildJsonObject {
                        put("id", JsonValues.ANY_STRING)
                        put("name", "MomCorp ID")
                        put("clientId", "test-client-id")
                        put("authorizationUrl", "https://nimbus.example/authorize")
                        put("tokenUrl", "https://nimbus.example/token")
                        put("userInfoUrl", "https://nimbus.example/userinfo")
                        put("userIdAttribute", "sub")
                        putJsonArray("scopes") { }
                    }
                )

            aggregateTemplate.findAll<OAuthProvider>().shouldBeSingle().scopes.shouldBeEmpty()
        }

        @Test
        fun `should return PROVIDER_ALREADY_EXISTS error when name is taken ignoring case`() {
            preconditions {
                oauthProvider(name = "Nimbus Auth")
            }

            client.graphqlMutation {
                createOAuthProviderMutation(name = "NIMBUS auth")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessError(
                    message = "OAuth provider with name 'NIMBUS auth' already exists",
                    errorCode = "PROVIDER_ALREADY_EXISTS",
                    path = DgsConstants.MUTATION.CreateOAuthProvider,
                )

            aggregateTemplate.findAll<OAuthProvider>().shouldBeSingle()
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["", "blank scope", "duplicate"])
        fun `should return INVALID_SCOPE error for malformed scopes`(scopeCase: String) {
            val scopes = when (scopeCase) {
                "duplicate" -> listOf("openid", "openid")
                else -> listOf("openid", scopeCase)
            }

            client.graphqlMutation {
                createOAuthProviderMutation(scopes = scopes)
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessErrorCode(
                    errorCode = "INVALID_SCOPE",
                    path = DgsConstants.MUTATION.CreateOAuthProvider,
                )

            aggregateTemplate.findAll<OAuthProvider>().shouldBeEmpty()
        }

        @Test
        fun `should return PROVIDER_ALREADY_EXISTS error when name is taken`() {
            preconditions {
                oauthProvider(name = "Nimbus Auth")
            }

            client.graphqlMutation {
                createOAuthProviderMutation(name = "Nimbus Auth")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessError(
                    message = "OAuth provider with name 'Nimbus Auth' already exists",
                    errorCode = "PROVIDER_ALREADY_EXISTS",
                    path = DgsConstants.MUTATION.CreateOAuthProvider,
                )

            aggregateTemplate.findAll<OAuthProvider>().shouldBeSingle()
        }
    }

    private fun MutationProjection.createOAuthProviderMutation(
        name: String = "Nimbus Auth",
        clientId: String = "test-client-id",
        clientSecret: String = "test-client-secret",
        authorizationUrl: String = "https://nimbus.example/authorize",
        tokenUrl: String = "https://nimbus.example/token",
        userInfoUrl: String = "https://nimbus.example/userinfo",
        userIdAttribute: String = "sub",
        scopes: List<String> = listOf("openid"),
    ) = createOAuthProvider(
        name = name,
        clientId = clientId,
        clientSecret = clientSecret,
        authorizationUrl = authorizationUrl,
        tokenUrl = tokenUrl,
        userInfoUrl = userInfoUrl,
        userIdAttribute = userIdAttribute,
        scopes = scopes,
    ) {
        id
        this.name
        this.clientId
        this.authorizationUrl
        this.tokenUrl
        this.userInfoUrl
        this.userIdAttribute
        this.scopes
    }
}
