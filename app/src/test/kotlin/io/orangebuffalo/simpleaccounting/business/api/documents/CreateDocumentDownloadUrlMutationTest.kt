package io.orangebuffalo.simpleaccounting.business.api.documents

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.documents.DocumentDownloadMetadata
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.integration.TokensRepository
import io.orangebuffalo.simpleaccounting.business.integration.getRequestByToken
import io.orangebuffalo.simpleaccounting.business.integration.downloads.PersistentDownloadRequest
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ContentDisposition
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.nio.charset.StandardCharsets

@DisplayName("createDocumentDownloadUrl mutation")
class CreateDocumentDownloadUrlMutationTest(
    @Autowired private val client: ApiTestClient,
    @Autowired private val tokensRepository: TokensRepository,
    @Autowired private val webTestClient: WebTestClient,
    @Autowired private val testDocumentsStorage: TestDocumentsStorage,
    @Value("\${local.server.port}") private val serverPort: Int,
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
            whenever(tokenGenerator.generateToken(argThat<Int> { this == 30 })) doReturn "test-token"

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
                        put("url", "http://localhost:$serverPort/api/documents/download/test-token")
                    }
                )
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error for admin user`() {
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
            whenever(tokenGenerator.generateToken(argThat<Int> { this == 30 })) doReturn "test-token"

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
                        put("url", "http://localhost:$serverPort/api/documents/download/test-token")
                    }
                )
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create download URL and store token with proper value and expiration`() {
            whenever(tokenGenerator.generateToken(argThat<Int> { this == 30 })) doReturn "generated-download-token"

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
                        put("url", "http://localhost:$serverPort/api/documents/download/generated-download-token")
                    }
                )

            val storedRequest = runBlocking {
                tokensRepository.getRequestByToken<PersistentDownloadRequest>("generated-download-token")
            }
            storedRequest.providerId.shouldBe(DocumentsService::class.simpleName!!)
            storedRequest.metadata.shouldBe(DocumentDownloadMetadata(preconditions.coffeeReceipt.id!!))
            storedRequest.userName.shouldBe("Fry")
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

        @Test
        fun `should allow anonymous download of document content via generated URL`() {
            val documentContent = "Good news, everyone! Dark matter delivery receipt"
            val downloadPreconditions = preconditions {
                object {
                    val leela = platformUser(
                        userName = "Leela",
                        documentsStorage = TestDocumentsStorage.STORAGE_ID,
                    )
                    val workspace = workspace(owner = leela)
                    val document = document(
                        workspace = workspace,
                        name = "dark-matter-receipt.pdf",
                        storageId = TestDocumentsStorage.STORAGE_ID,
                        storageLocation = "leela-receipt-location",
                        sizeInBytes = documentContent.toByteArray().size.toLong(),
                        mimeType = "application/pdf",
                    )
                }
            }
            testDocumentsStorage.mockDocumentContent(
                "leela-receipt-location",
                documentContent.toByteArray(),
            )

            val mutationResponseBody = client
                .graphqlMutation {
                    createDocumentDownloadUrlMutation(
                        workspaceId = downloadPreconditions.workspace.id!!,
                        documentId = downloadPreconditions.document.id!!,
                    )
                }
                .from(downloadPreconditions.leela)
                .execute()
                .expectStatus().isOk
                .expectBody<String>()
                .returnResult()
                .responseBody
                .shouldNotBeNull()

            val downloadUrl = Json.parseToJsonElement(mutationResponseBody)
                .jsonObject["data"]?.jsonObject
                ?.get("createDocumentDownloadUrl")?.jsonObject
                ?.get("url")?.jsonPrimitive?.content
                .shouldNotBeNull()
            val downloadPath = downloadUrl.removePrefix("http://localhost:$serverPort")

            webTestClient.get()
                .uri(downloadPath)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentDisposition(
                    ContentDisposition.parse("attachment; filename=\"dark-matter-receipt.pdf\"")
                )
                .expectHeader().contentType(MediaType.APPLICATION_PDF)
                .expectBody()
                .consumeWith { downloadResponse ->
                    val downloadedContent = downloadResponse.responseBody.shouldNotBeNull()
                    String(downloadedContent, StandardCharsets.UTF_8).shouldBe(documentContent)
                }
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
