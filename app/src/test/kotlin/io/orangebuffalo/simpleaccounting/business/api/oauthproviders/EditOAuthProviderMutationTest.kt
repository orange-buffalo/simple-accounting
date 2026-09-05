package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProvider
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
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
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("editOAuthProvider mutation")
class EditOAuthProviderMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val farnsworth = farnsworth()
            val fry = fry()
            val nimbus = oauthProvider(
                name = "Nimbus Auth",
                clientId = "nimbus-client-id",
                clientSecret = "nimbus-client-secret",
                scopes = setOf("openid"),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphqlMutation { editOAuthProviderMutation() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditOAuthProvider)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for regular user`() {
            client.graphqlMutation { editOAuthProviderMutation() }
                .from(preconditions.fry)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditOAuthProvider)
        }
    }

    @Nested
    @DisplayName("Input Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("name") { value -> editOAuthProviderMutation(name = value) },
            sizeConstraintTestCases("name", maxLength = 255) { value -> editOAuthProviderMutation(name = value) },
            mustNotBeBlankTestCases("clientId") { value -> editOAuthProviderMutation(clientId = value) },
            sizeConstraintTestCases("clientSecret", maxLength = 255, minLength = 1) { value ->
                editOAuthProviderMutation(clientSecret = value)
            },
            mustNotBeBlankTestCases("userIdAttribute") { value -> editOAuthProviderMutation(userIdAttribute = value) },
            requiredFieldRejectedTestCases("version") { editOAuthProviderMutation() },
            optionalFieldAbsentTestCases("clientSecret") { editOAuthProviderMutation() },
            // the endpoints are URLs, so the generic length boundaries do not apply to them
            endpointUrlFieldTestCases("authorizationUrl") { value ->
                editOAuthProviderMutation(authorizationUrl = value)
            },
            endpointUrlFieldTestCases("tokenUrl") { value -> editOAuthProviderMutation(tokenUrl = value) },
            endpointUrlFieldTestCases("userInfoUrl") { value -> editOAuthProviderMutation(userInfoUrl = value) },
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
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditOAuthProvider)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should update the provider registration`() {
            client.graphqlMutation {
                editOAuthProviderMutation(
                    name = "Nimbus SSO",
                    clientId = "new-client-id",
                    clientSecret = "new-client-secret",
                    authorizationUrl = "https://sso.nimbus.example/authorize",
                    tokenUrl = "https://sso.nimbus.example/token",
                    userInfoUrl = "https://sso.nimbus.example/userinfo",
                    userIdAttribute = "email",
                    scopes = listOf("openid", "profile"),
                )
            }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditOAuthProvider to buildJsonObject {
                        put("id", preconditions.nimbus.id!!)
                        put("version", 1)
                        put("name", "Nimbus SSO")
                        put("clientId", "new-client-id")
                        put("authorizationUrl", "https://sso.nimbus.example/authorize")
                        put("tokenUrl", "https://sso.nimbus.example/token")
                        put("userInfoUrl", "https://sso.nimbus.example/userinfo")
                        put("userIdAttribute", "email")
                        putJsonArray("scopes") {
                            add(JsonPrimitive("openid"))
                            add(JsonPrimitive("profile"))
                        }
                    }
                )

            val provider = aggregateTemplate.findSingle<OAuthProvider>(preconditions.nimbus.id!!)
            provider.name.shouldBe("Nimbus SSO")
            provider.clientId.shouldBe("new-client-id")
            provider.clientSecret.shouldBe("new-client-secret")
            provider.scopes.map { it.scope }.shouldContainExactlyInAnyOrder("openid", "profile")
        }

        @Test
        fun `should keep the current client secret when it is not provided`() {
            client.graphqlMutation {
                editOAuthProviderMutation(name = "Nimbus SSO", clientSecret = null)
            }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditOAuthProvider to buildJsonObject {
                        put("id", preconditions.nimbus.id!!)
                        put("version", 1)
                        put("name", "Nimbus SSO")
                        put("clientId", "nimbus-client-id")
                        put("authorizationUrl", "https://nimbus.example/authorize")
                        put("tokenUrl", "https://nimbus.example/token")
                        put("userInfoUrl", "https://nimbus.example/userinfo")
                        put("userIdAttribute", "sub")
                        putJsonArray("scopes") {
                            add(JsonPrimitive("openid"))
                        }
                    }
                )

            aggregateTemplate.findSingle<OAuthProvider>(preconditions.nimbus.id!!)
                .clientSecret.shouldBe("nimbus-client-secret")
        }

        @Test
        fun `should accept the current name of the provider`() {
            client.graphqlMutation {
                editOAuthProviderMutation(name = "Nimbus Auth", clientId = "rotated-client-id")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditOAuthProvider to buildJsonObject {
                        put("id", preconditions.nimbus.id!!)
                        put("version", 1)
                        put("name", "Nimbus Auth")
                        put("clientId", "rotated-client-id")
                        put("authorizationUrl", "https://nimbus.example/authorize")
                        put("tokenUrl", "https://nimbus.example/token")
                        put("userInfoUrl", "https://nimbus.example/userinfo")
                        put("userIdAttribute", "sub")
                        putJsonArray("scopes") {
                            add(JsonPrimitive("openid"))
                        }
                    }
                )
        }

        @Test
        fun `should remove all scopes when an empty list is provided`() {
            client.graphqlMutation {
                editOAuthProviderMutation(scopes = emptyList())
            }
                .from(preconditions.farnsworth)
                .execute()
                .expectStatus().isOk

            aggregateTemplate.findSingle<OAuthProvider>(preconditions.nimbus.id!!)
                .scopes.shouldBeEmpty()
        }

        @Test
        fun `should allow changing the user id attribute while no identities are linked`() {
            client.graphqlMutation {
                editOAuthProviderMutation(userIdAttribute = "email")
            }
                .from(preconditions.farnsworth)
                .execute()
                .expectStatus().isOk

            aggregateTemplate.findSingle<OAuthProvider>(preconditions.nimbus.id!!)
                .userIdAttribute.shouldBe("email")
        }

        @Test
        fun `should return USER_ID_ATTRIBUTE_LOCKED error when identities are already linked`() {
            preconditions {
                userOAuthIdentity(provider = preconditions.nimbus, externalId = "fry-at-nimbus")
            }

            client.graphqlMutation {
                editOAuthProviderMutation(userIdAttribute = "email")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessError(
                    message = "User ID attribute cannot be changed while 1 identities are linked to this provider",
                    errorCode = "USER_ID_ATTRIBUTE_LOCKED",
                    path = DgsConstants.MUTATION.EditOAuthProvider,
                )

            aggregateTemplate.findSingle<OAuthProvider>(preconditions.nimbus.id!!)
                .userIdAttribute.shouldBe("sub")
        }

        @Test
        fun `should allow editing other attributes while identities are linked`() {
            preconditions {
                userOAuthIdentity(provider = preconditions.nimbus, externalId = "fry-at-nimbus")
            }

            client.graphqlMutation {
                editOAuthProviderMutation(clientId = "rotated-client-id")
            }
                .from(preconditions.farnsworth)
                .execute()
                .expectStatus().isOk

            aggregateTemplate.findSingle<OAuthProvider>(preconditions.nimbus.id!!)
                .clientId.shouldBe("rotated-client-id")
        }

        @Test
        fun `should return INVALID_SCOPE error for malformed scopes`() {
            client.graphqlMutation {
                editOAuthProviderMutation(scopes = listOf("openid", "openid"))
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessErrorCode(
                    errorCode = "INVALID_SCOPE",
                    path = DgsConstants.MUTATION.EditOAuthProvider,
                )
        }

        @Test
        fun `should return PROVIDER_ALREADY_EXISTS error when name is taken by another provider ignoring case`() {
            preconditions {
                oauthProvider(name = "MomCorp ID")
            }

            client.graphqlMutation {
                editOAuthProviderMutation(name = "momcorp id")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessError(
                    message = "OAuth provider with name 'momcorp id' already exists",
                    errorCode = "PROVIDER_ALREADY_EXISTS",
                    path = DgsConstants.MUTATION.EditOAuthProvider,
                )
        }

        @Test
        fun `should return PROVIDER_ALREADY_EXISTS error when name is taken by another provider`() {
            preconditions {
                oauthProvider(name = "MomCorp ID")
            }

            client.graphqlMutation {
                editOAuthProviderMutation(name = "MomCorp ID")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessError(
                    message = "OAuth provider with name 'MomCorp ID' already exists",
                    errorCode = "PROVIDER_ALREADY_EXISTS",
                    path = DgsConstants.MUTATION.EditOAuthProvider,
                )

            aggregateTemplate.findSingle<OAuthProvider>(preconditions.nimbus.id!!)
                .name.shouldBe("Nimbus Auth")
        }

        @Test
        fun `should return SUBMITTED_OUTDATED_STATE error for stale version`() {
            client.graphqlMutation {
                editOAuthProviderMutation(version = 42)
            }
                .from(preconditions.farnsworth)
                .executeAndVerifySubmittedOutdatedStateError(path = DgsConstants.MUTATION.EditOAuthProvider)
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error for unknown provider`() {
            client.graphqlMutation {
                editOAuthProviderMutation(id = "unknown-id")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditOAuthProvider)
        }
    }

    private fun MutationProjection.editOAuthProviderMutation(
        id: String = preconditions.nimbus.id!!,
        version: Int = 0,
        name: String = "Nimbus Auth",
        clientId: String = "nimbus-client-id",
        clientSecret: String? = "nimbus-client-secret",
        authorizationUrl: String = "https://nimbus.example/authorize",
        tokenUrl: String = "https://nimbus.example/token",
        userInfoUrl: String = "https://nimbus.example/userinfo",
        userIdAttribute: String = "sub",
        scopes: List<String> = listOf("openid"),
    ) = editOAuthProvider(
        id = id,
        version = version,
        name = name,
        clientId = clientId,
        clientSecret = clientSecret,
        authorizationUrl = authorizationUrl,
        tokenUrl = tokenUrl,
        userInfoUrl = userInfoUrl,
        userIdAttribute = userIdAttribute,
        scopes = scopes,
    ) {
        this.id
        this.version
        this.name
        this.clientId
        this.authorizationUrl
        this.tokenUrl
        this.userInfoUrl
        this.userIdAttribute
        this.scopes
    }
}
