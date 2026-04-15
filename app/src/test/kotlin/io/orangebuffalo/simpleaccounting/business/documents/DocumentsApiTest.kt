package io.orangebuffalo.simpleaccounting.business.documents

import io.kotest.matchers.equals.shouldBeEqual
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBodyEqualTo
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME_VALUE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.toDataBuffers
import kotlinx.serialization.json.put
import net.javacrumbs.jsonunit.kotest.inPath
import net.javacrumbs.jsonunit.kotest.shouldBeJsonString
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.util.InMemoryResource
import org.springframework.test.web.reactive.server.WebTestClient
import java.nio.charset.StandardCharsets

@DisplayName("Documents API ")
class DocumentsApiTest(
    @Autowired private val client: WebTestClient,
    @Autowired private val testDocumentsStorage: DocumentsStorageMockHolder,
) : SaIntegrationTestBase() {

    @BeforeEach
    fun setup() {
        whenever(testDocumentsStorage.mock.getId()) doReturn "mocked-storage"
    }

    @Test
    fun `should allow GET access for document content only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents/${preconditions.coffeeReceipt.id}/content")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should GET document content`() {
        testDocumentsStorage.mock.stub {
            onBlocking {
                getDocumentContent(preconditions.fryWorkspace, "test-location")
            } doReturn "test-content".toDataBuffers()
        }

        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents/${preconditions.coffeeReceipt.id}/content")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentDisposition(ContentDisposition.parse("attachment; filename=\"100_cups.pdf\""))
            .expectHeader().contentLength(42)
            .expectHeader().contentType(MediaType.APPLICATION_PDF)
            .expectBody()
            .consumeWith { exchange ->
                exchange.responseBody.shouldNotBeNull().also { body ->
                    val text = String(body, StandardCharsets.UTF_8)
                    text.shouldBe("test-content")
                }
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting document content`() {
        client.get()
            .uri("/api/workspaces/5634632/documents/${preconditions.coffeeReceipt.id}/content")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting document content`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents/${preconditions.coffeeReceipt.id}/content")
            .verifyNotFound("Workspace ${preconditions.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if document belongs to another workspace when requesting document content`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents/${preconditions.anotherFryWorkspaceDocument.id}/content")
            .verifyNotFound("Document ${preconditions.anotherFryWorkspaceDocument.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating document`() {
        client.post()
            .uri("/api/workspaces/995943/documents")
            .bodyValue(preconditions.createDefaultFileToUpload().build())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating document`() {
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents")
            .bodyValue(preconditions.createDefaultFileToUpload().build())
            .verifyNotFound("Workspace ${preconditions.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should upload a new file and invoke documents storage`() {
        mockDocumentsStorage(preconditions.fryWorkspace)

        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents")
            .bodyValue(preconditions.createDefaultFileToUpload().build())
            .verifyOkAndJsonBodyEqualTo {
                put("name", "test-file.txt")
                put("id", "${JsonValues.ANY_NUMBER}")
                put("version", 0)
                put("timeUploaded", MOCK_TIME_VALUE)
                put("sizeInBytes", 42)
                put("storageId", "mocked-storage")
                put("mimeType", "text/plain")
            }
    }

    private fun mockDocumentsStorage(fryWorkspace: Workspace) {
        testDocumentsStorage.mock.stub {
            onBlocking {
                saveDocument(argThat { workspace == fryWorkspace && fileName == "test-file.txt" })
            } doReturn SaveDocumentResponse(
                storageLocation = "test-location",
                sizeInBytes = 42
            )
        }
    }

    @Test
    fun `should allow GET access for document download token only for logged in user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents/${preconditions.coffeeReceipt.id}/download-token")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting document download token`() {
        client.get()
            .uri("/api/workspaces/5634632/documents/${preconditions.coffeeReceipt.id}/download-token")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting document download token`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents/${preconditions.coffeeReceipt.id}/download-token")
            .verifyNotFound("Workspace ${preconditions.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if document belongs to another workspace when requesting document download token`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents/${preconditions.anotherFryWorkspaceDocument.id}/download-token")
            .verifyNotFound("Document ${preconditions.anotherFryWorkspaceDocument.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return document download token`() {
        whenever(tokenGenerator.generateToken(any())) doReturn "token"

        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents/${preconditions.coffeeReceipt.id}/download-token")
            .verifyOkAndJsonBody {
                inPath("token").shouldBeJsonString().shouldBeEqual("token")
            }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(userName = "Fry", documentsStorage = "mocked-storage")
            val fryWorkspace = workspace(owner = fry)
            val anotherFryWorkspace = workspace(owner = fry)
            val anotherFryWorkspaceDocument = document(workspace = anotherFryWorkspace)
            val farnsworth = farnsworth()
            val coffeeReceipt = document(
                name = "100_cups.pdf",
                workspace = fryWorkspace,
                storageId = "mocked-storage",
                storageLocation = "test-location",
                timeUploaded = MOCK_TIME,
                sizeInBytes = 42,
                mimeType = "application/pdf"
            )

            fun createDefaultFileToUpload(): MultipartBodyBuilder = MultipartBodyBuilder()
                .apply {
                    part(
                        "file",
                        InMemoryResource("test-content"),
                        MediaType.TEXT_PLAIN
                    ).header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                            .builder("form-data")
                            .name("file")
                            .filename("test-file.txt")
                            .build().toString()
                    )
                }
        }
    }

    class DocumentsStorageMockHolder(
        val mock: DocumentsStorage = mock(DocumentsStorage::class.java)
    ) : DocumentsStorage by mock

    @TestConfiguration
    class DocumentControllerTestConfig {
        @Bean
        fun documentsStorageMockHolder() = DocumentsStorageMockHolder()
    }
}
