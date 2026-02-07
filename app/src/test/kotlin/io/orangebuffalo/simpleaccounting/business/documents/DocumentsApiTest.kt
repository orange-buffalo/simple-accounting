package io.orangebuffalo.simpleaccounting.business.documents

import io.kotest.matchers.equals.shouldBeEqual
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.tests.infra.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBodyEqualTo
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import net.javacrumbs.jsonunit.kotest.inPath
import net.javacrumbs.jsonunit.kotest.shouldBeJsonString
import org.assertj.core.api.Assertions.assertThat
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
import java.util.function.Consumer

@DisplayName("Documents API ")
class DocumentsApiTest(
    @Autowired private val client: WebTestClient,
    @Autowired private val testDocumentsStorage: DocumentsStorageMockHolder,
) : SaIntegrationTestBase() {

    @BeforeEach
    fun setup() {
        whenever(testDocumentsStorage.mock.getId()) doReturn "mocked-storage"
        mockCurrentTime(timeService)
    }

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return documents of a workspace of current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents")
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 1)
                put("pageSize", 10)
                put("totalElements", 2)
                putJsonArray("data") {
                    addJsonObject {
                        put("name", "unknown")
                        put("id", preconditions.cheesePizzaAndALargeSodaReceipt.id)
                        put("version", 0)
                        put("timeUploaded", MOCK_TIME_VALUE)
                        put("sizeInBytes", JsonNull)
                    }
                    addJsonObject {
                        put("name", "100_cups.pdf")
                        put("id", preconditions.coffeeReceipt.id)
                        put("version", 0)
                        put("timeUploaded", MOCK_TIME_VALUE)
                        put("sizeInBytes", 42)
                    }
                }
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found on GET`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

        client.get()
            .uri("/api/workspaces/27347947239/documents")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/documents")
            .verifyNotFound("Workspace ${preconditions.fryWorkspace.id} is not found")
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
            // todo: #108
            //.expectHeader().contentType(MediaType.APPLICATION_PDF)
            .expectBody()
            .consumeWith { exchange ->
                assertThat(exchange.responseBody).isNotNull.satisfies(Consumer { body ->
                    val text = String(body, StandardCharsets.UTF_8)
                    assertThat(text).isEqualTo("test-content")
                })
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
    @WithMockFryUser
    fun `should filter documents by ids`() {
        client.get()
            .uri { builder ->
                builder.replacePath("/api/workspaces/${preconditions.fryWorkspace.id}/documents")
                    .queryParam("id[eq]", "${preconditions.cheesePizzaAndALargeSodaReceipt.id}")
                    .build()
            }
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 1)
                put("pageSize", 10)
                put("totalElements", 1)
                putJsonArray("data") {
                    addJsonObject {
                        put("name", "unknown")
                        put("id", preconditions.cheesePizzaAndALargeSodaReceipt.id)
                        put("version", 0)
                        put("timeUploaded", MOCK_TIME_VALUE)
                        put("sizeInBytes", JsonNull)
                    }
                }
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
                sizeInBytes = 42
            )
            val cheesePizzaAndALargeSodaReceipt = document(
                name = "unknown",
                workspace = fryWorkspace,
                timeUploaded = MOCK_TIME,
                sizeInBytes = null
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
