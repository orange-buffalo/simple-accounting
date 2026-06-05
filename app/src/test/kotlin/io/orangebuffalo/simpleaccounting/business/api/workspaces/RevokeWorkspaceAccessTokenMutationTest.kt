package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.workspaces.SavedWorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("revokeWorkspaceAccessToken mutation")
class RevokeWorkspaceAccessTokenMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val zoidberg = zoidberg()
            val fryWorkspace = workspace(owner = fry)
            val zoidbergWorkspace = workspace(owner = zoidberg)
            val fryWorkspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                token = "planet-express-auth-temporary-link",
                validTill = MOCK_TIME.plusSeconds(10_000),
            )
            val zoidbergWorkspaceAccessToken = workspaceAccessToken(
                workspace = zoidbergWorkspace,
                token = "decapodian-auth-temporary-link",
                validTill = MOCK_TIME.plusSeconds(10_000),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation { revokeWorkspaceAccessTokenMutation(preconditions.fryWorkspaceAccessToken.id!!) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.RevokeWorkspaceAccessToken)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { revokeWorkspaceAccessTokenMutation(preconditions.fryWorkspaceAccessToken.id!!) }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.RevokeWorkspaceAccessToken)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { revokeWorkspaceAccessTokenMutation(preconditions.fryWorkspaceAccessToken.id!!) }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.RevokeWorkspaceAccessToken)
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error when token belongs to another user workspace`() {
            client
                .graphqlMutation { revokeWorkspaceAccessTokenMutation(preconditions.zoidbergWorkspaceAccessToken.id!!) }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.RevokeWorkspaceAccessToken)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should revoke and remove workspace access token`() {
            val testData = preconditions {
                object {
                    val accessToken = workspaceAccessToken(
                        workspace = preconditions.fryWorkspace,
                        token = "planet-express-temporary-link",
                        validTill = MOCK_TIME.plusSeconds(10_000),
                    )
                }
            }

            client
                .graphqlMutation { revokeWorkspaceAccessTokenMutation(testData.accessToken.id!!) }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.MUTATION.RevokeWorkspaceAccessToken to JsonPrimitive(true)
                )

            aggregateTemplate.findAll<WorkspaceAccessToken>()
                .filter { it.id == testData.accessToken.id }
                .shouldBe(emptyList())
        }

        @Test
        fun `should remove saved shared links before revoking workspace access token`() {
            val testData = preconditions {
                object {
                    val accessToken = workspaceAccessToken(
                        workspace = preconditions.fryWorkspace,
                        token = "planet-express-saved-temporary-link",
                        validTill = MOCK_TIME.plusSeconds(10_000),
                    ).also { accessToken ->
                        aggregateTemplate.save(
                            SavedWorkspaceAccessToken(
                                ownerId = preconditions.zoidberg.id!!,
                                workspaceAccessTokenId = accessToken.id!!,
                            )
                        )
                    }
                }
            }

            client
                .graphqlMutation { revokeWorkspaceAccessTokenMutation(testData.accessToken.id!!) }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.MUTATION.RevokeWorkspaceAccessToken to JsonPrimitive(true)
                )

            aggregateTemplate.findAll<SavedWorkspaceAccessToken>()
                .filter { it.workspaceAccessTokenId == testData.accessToken.id }
                .shouldBe(emptyList())
            aggregateTemplate.findAll<WorkspaceAccessToken>()
                .filter { it.id == testData.accessToken.id }
                .shouldBe(emptyList())
        }
    }

    private fun MutationProjection.revokeWorkspaceAccessTokenMutation(
        accessTokenId: String,
    ): MutationProjection = revokeWorkspaceAccessToken(accessTokenId = accessTokenId)
}
