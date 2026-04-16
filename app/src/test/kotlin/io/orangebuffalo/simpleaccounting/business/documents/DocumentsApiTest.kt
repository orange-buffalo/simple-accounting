package io.orangebuffalo.simpleaccounting.business.documents

import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBodyEqualTo
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME_VALUE
import kotlinx.serialization.json.put
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

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(userName = "Fry", documentsStorage = "mocked-storage")
            val fryWorkspace = workspace(owner = fry)
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
