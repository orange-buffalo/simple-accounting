package io.orangebuffalo.simpleaccounting.business.api.documents

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("createDocumentDownloadUrl mutation")
class CreateDocumentDownloadUrlMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(userName = "Fry", documentsStorage = "noop")
            val farnsworth = platformUser(userName = "Farnsworth", isAdmin = true, documentsStorage = "noop")
            val fryWorkspace = workspace(owner = fry)
            val coffeeReceipt = document(workspace = fryWorkspace, name = "100_cups.pdf")
            val workspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
            val zoidberg = platformUser(userName = "Zoidberg", documentsStorage = "noop")
            val zoidbergWorkspace = workspace(owner = zoidberg)
            val zoidbergDocument = document(workspace = zoidbergWorkspace)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation {
                    createDocumentDownloadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        documentId = preconditions.coffeeReceipt.id!!,
                    )
                }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateDocumentDownloadUrl)
        }

        @Test
        fun `should allow access for regular user`() {
            whenever(tokenGenerator.generateToken(any())) doReturn "test-token"

            client
                .graphqlMutation {
                    createDocumentDownloadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        documentId = preconditions.coffeeReceipt.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateDocumentDownloadUrl to buildJsonObject {
                        put("url", "https://simple-accounting.orange-buffalo.io/api/downloads?token=test-token")
                    }
                )
        }

        @Test
        fun `should allow access for admin user`() {
            whenever(tokenGenerator.generateToken(any())) doReturn "test-token"

            client
                .graphqlMutation {
                    createDocumentDownloadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        documentId = preconditions.coffeeReceipt.id!!,
                    )
                }
                .from(preconditions.farnsworth)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateDocumentDownloadUrl)
        }

        @Test
        fun `should allow access with workspace token`() {
            whenever(tokenGenerator.generateToken(any())) doReturn "test-token"

            client
                .graphqlMutation {
                    createDocumentDownloadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        documentId = preconditions.coffeeReceipt.id!!,
                    )
                }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateDocumentDownloadUrl to buildJsonObject {
                        put("url", "https://simple-accounting.orange-buffalo.io/api/downloads?token=test-token")
                    }
                )
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create download URL with absolute path`() {
            whenever(tokenGenerator.generateToken(any())) doReturn "generated-download-token"

            client
                .graphqlMutation {
                    createDocumentDownloadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        documentId = preconditions.coffeeReceipt.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateDocumentDownloadUrl to buildJsonObject {
                        put("url", "https://simple-accounting.orange-buffalo.io/api/downloads?token=generated-download-token")
                    }
                )
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error when workspace is not found`() {
            client
                .graphqlMutation {
                    createDocumentDownloadUrlMutation(
                        workspaceId = 5634632,
                        documentId = preconditions.coffeeReceipt.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateDocumentDownloadUrl)
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error when workspace belongs to another user`() {
            client
                .graphqlMutation {
                    createDocumentDownloadUrlMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                        documentId = preconditions.zoidbergDocument.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateDocumentDownloadUrl)
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error when document belongs to another workspace`() {
            client
                .graphqlMutation {
                    createDocumentDownloadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        documentId = preconditions.zoidbergDocument.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateDocumentDownloadUrl)
        }
    }

    private fun MutationProjection.createDocumentDownloadUrlMutation(
        workspaceId: Long,
        documentId: Long,
    ): MutationProjection = createDocumentDownloadUrl(
        workspaceId = workspaceId,
        documentId = documentId,
    ) {
        url
    }
}
