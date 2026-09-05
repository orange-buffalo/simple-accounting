package io.orangebuffalo.simpleaccounting.business.api.profile

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.oauthproviders.UserOAuthIdentity
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("unlinkOAuthIdentity mutation")
class UnlinkOAuthIdentityMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val nimbus = oauthProvider(name = "Nimbus Auth")
            val fry = fry().also {
                userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
            }
            val bender = bender().also {
                userOAuthIdentity(user = it, provider = nimbus, externalId = "bender-at-nimbus")
            }
            val zoidberg = zoidberg()
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
            client.graphqlMutation { unlinkOAuthIdentityMutation() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.UnlinkOAuthIdentity)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client.graphqlMutation { unlinkOAuthIdentityMutation() }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.UnlinkOAuthIdentity)
        }
    }

    @Nested
    @DisplayName("Input Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputValidation {
        // valid boundary values are not applicable here: any non-blank provider id
        // still has to resolve to a linked identity
        fun testCases() = mustNotBeBlankTestCases("providerId") { value ->
            unlinkOAuthIdentityMutation(providerId = value)
        }.filterNot { it is GraphqlMutationValidBoundaryTestCase }

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client.buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.UnlinkOAuthIdentity)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should unlink the identity of the current user only`() {
            client.graphqlMutation { unlinkOAuthIdentityMutation() }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.UnlinkOAuthIdentity to buildJsonObject {
                        put("success", true)
                    }
                )

            aggregateTemplate.findAll<UserOAuthIdentity>()
                .shouldBeSingle()
                .userId.shouldBe(preconditions.bender.id)
        }

        @Test
        fun `should keep the other identities when one of several is unlinked`() {
            val testData = preconditions {
                object {
                    val momCorp = oauthProvider(name = "MomCorp ID").also {
                        userOAuthIdentity(user = preconditions.fry, provider = it, externalId = "fry-at-momcorp")
                    }
                }
            }

            client.graphqlMutation { unlinkOAuthIdentityMutation() }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.UnlinkOAuthIdentity to buildJsonObject {
                        put("success", true)
                    }
                )

            withHint("Password login stays unavailable while another identity is linked") {
                aggregateTemplate.findAll<UserOAuthIdentity>()
                    .filter { it.userId == preconditions.fry.id }
                    .shouldBeSingle()
                    .providerId.shouldBe(testData.momCorp.id)
            }
        }

        @Test
        fun `should restore password login when the last identity is unlinked`() {
            client.graphqlMutation { unlinkOAuthIdentityMutation() }
                .from(preconditions.fry)
                .execute()
                .expectStatus().isOk

            client.graphql {
                userAuthenticationMethods(userName = preconditions.fry.userName) {
                    type
                    providerId
                    providerName
                }
            }
                .fromAnonymous()
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.UserAuthenticationMethods to buildJsonArray {
                        add(buildJsonObject {
                            put("type", "PASSWORD")
                            put("providerId", null as String?)
                            put("providerName", null as String?)
                        })
                    }
                )
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error when nothing is linked at the provider`() {
            client.graphqlMutation { unlinkOAuthIdentityMutation() }
                .from(preconditions.zoidberg)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.UnlinkOAuthIdentity)

            aggregateTemplate.findAll<UserOAuthIdentity>().size.shouldBe(2)
        }
    }

    private fun MutationProjection.unlinkOAuthIdentityMutation(
        providerId: String = preconditions.nimbus.id!!,
    ) = unlinkOAuthIdentity(providerId = providerId) {
        success
    }
}
