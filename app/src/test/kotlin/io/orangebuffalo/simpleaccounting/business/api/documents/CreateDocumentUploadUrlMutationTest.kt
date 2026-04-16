package io.orangebuffalo.simpleaccounting.business.api.documents

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integration.uploads.UploadsRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@DisplayName("createDocumentUploadUrl mutation")
class CreateDocumentUploadUrlMutationTest(
    @Autowired private val client: ApiTestClient,
    @Autowired private val uploadsRepository: UploadsRepository,
    @Value("\${local.server.port}") private val serverPort: Int,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
            val farnsworth = platformUser(userName = "Farnsworth", isAdmin = true, documentsStorage = "test-storage")
            val fryWorkspace = workspace(owner = fry)
            val workspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
            val zoidberg = platformUser(userName = "Zoidberg", documentsStorage = "test-storage")
            val zoidbergWorkspace = workspace(owner = zoidberg)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation {
                    createDocumentUploadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                    )
                }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateDocumentUploadUrl)
        }

        @Test
        fun `should allow access for regular user`() {
            whenever(tokenGenerator.generateToken(argThat<Int> { this == 30 })) doReturn "test-token"

            client
                .graphqlMutation {
                    createDocumentUploadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateDocumentUploadUrl to buildJsonObject {
                        put("url", "http://localhost:$serverPort/api/documents/upload/test-token")
                        put("filePartName", "file")
                    }
                )
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation {
                    createDocumentUploadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                    )
                }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateDocumentUploadUrl)
        }

        @Test
        fun `should return NOT_AUTHORIZED error with workspace token`() {
            client
                .graphqlMutation {
                    createDocumentUploadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                    )
                }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateDocumentUploadUrl)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create upload URL and store token with proper workspace and expiration`() {
            whenever(tokenGenerator.generateToken(argThat<Int> { this == 30 })) doReturn "generated-upload-token"

            client
                .graphqlMutation {
                    createDocumentUploadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateDocumentUploadUrl to buildJsonObject {
                        put("url", "http://localhost:$serverPort/api/documents/upload/generated-upload-token")
                        put("filePartName", "file")
                    }
                )

            val storedRequest = runBlocking {
                uploadsRepository.getRequestByToken("generated-upload-token")
            }
            storedRequest.workspaceId.shouldBe(preconditions.fryWorkspace.id)
            storedRequest.userName.shouldBe("Fry")
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error when workspace is not found`() {
            client
                .graphqlMutation {
                    createDocumentUploadUrlMutation(
                        workspaceId = 5634632,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateDocumentUploadUrl)
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error when workspace belongs to another user`() {
            client
                .graphqlMutation {
                    createDocumentUploadUrlMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateDocumentUploadUrl)
        }

        @Test
        fun `should return url with filePartName field`() {
            whenever(tokenGenerator.generateToken(argThat<Int> { this == 30 })) doReturn "token-for-parts-test"

            client
                .graphqlMutation {
                    createDocumentUploadUrlMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateDocumentUploadUrl to buildJsonObject {
                        put("url", "http://localhost:$serverPort/api/documents/upload/token-for-parts-test")
                        put("filePartName", "file")
                    }
                )
        }
    }

    private fun MutationProjection.createDocumentUploadUrlMutation(
        workspaceId: Long,
    ): MutationProjection = createDocumentUploadUrl(
        workspaceId = workspaceId,
    ) {
        url
        filePartName
    }
}
