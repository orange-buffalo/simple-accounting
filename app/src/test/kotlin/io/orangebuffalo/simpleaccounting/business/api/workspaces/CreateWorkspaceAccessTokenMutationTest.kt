package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private val VALID_TILL: Instant =
    ZonedDateTime.of(3025, 1, 15, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant()
private const val VALID_TILL_VALUE = "3025-01-15T10:00:00Z"

@DisplayName("createWorkspaceAccessToken mutation")
class CreateWorkspaceAccessTokenMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val fryWorkspace = workspace(owner = fry)
            val workspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation { createWorkspaceAccessTokenMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateWorkspaceAccessToken)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { createWorkspaceAccessTokenMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateWorkspaceAccessToken)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { createWorkspaceAccessTokenMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateWorkspaceAccessToken)
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error when workspace belongs to another user`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val zoidberg = zoidberg()
                    val zoidbergWorkspace = workspace(owner = zoidberg)
                }
            }
            client
                .graphqlMutation {
                    createWorkspaceAccessTokenMutation(workspaceId = testData.zoidbergWorkspace.id!!)
                }
                .from(testData.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateWorkspaceAccessToken)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create a new workspace access token`() {
            whenever(tokenGenerator.generateToken()) doReturn "new-share-token"

            client
                .graphqlMutation {
                    createWorkspaceAccessTokenMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        validTill = VALID_TILL,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateWorkspaceAccessToken to buildJsonObject {
                        put("id", JsonValues.ANY_NUMBER)
                        put("version", 0)
                        put("token", "new-share-token")
                        put("validTill", VALID_TILL_VALUE)
                        put("revoked", false)
                    }
                )

            aggregateTemplate.findAll<WorkspaceAccessToken>()
                .filter { it.workspaceId == preconditions.fryWorkspace.id && it.token == "new-share-token" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    WorkspaceAccessToken(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        timeCreated = MOCK_TIME,
                        validTill = VALID_TILL,
                        revoked = false,
                        token = "new-share-token",
                    )
                )
        }
    }

    private fun MutationProjection.createWorkspaceAccessTokenMutation(
        workspaceId: Long,
        validTill: Instant = VALID_TILL,
    ): MutationProjection = createWorkspaceAccessToken(
        workspaceId = workspaceId,
        validTill = validTill,
    ) {
        this.id
        this.version
        this.token
        this.validTill
        this.revoked
    }
}
