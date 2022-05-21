package io.orangebuffalo.simpleaccounting.domain.documents

import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.*
import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.domain.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.integration.downloads.DownloadsService
import io.orangebuffalo.simpleaccounting.utils.toDataBuffers
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.util.InMemoryResource
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

@SimpleAccountingIntegrationTest
@DisplayName("Documents API ")
class DocumentsApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val testDocumentsStorage: TestDocumentsStorage
) {

    @MockBean
    lateinit var timeService: TimeService

    @MockBean
    lateinit var downloadsService: DownloadsService

    @BeforeEach
    fun setup() {
        whenever(testDocumentsStorage.mock.getId()) doReturn "test-storage"
        mockCurrentTime(timeService)
    }

    @Test
    fun `should allow GET access only for logged in users`(testData: DocumentsApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return documents of a workspace of current user`(testData: DocumentsApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")
                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                        "name": "unknown",
                        "id": ${testData.cheesePizzaAndALargeSodaReceipt.id},
                        "version": 0,
                        "timeUploaded": "$MOCK_TIME_VALUE",
                        "sizeInBytes": null
                    }"""
                    ),

                    json(
                        """{
                        "name": "100_cups.pdf",
                        "id": ${testData.coffeeReceipt.id},
                        "version": 0,
                        "timeUploaded": "$MOCK_TIME_VALUE",
                        "sizeInBytes": 42
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found on GET`(testData: DocumentsApiTestData) {
        client.get()
            .uri("/api/workspaces/27347947239/documents")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`(testData: DocumentsApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents")
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for document content only for logged in users`(testData: DocumentsApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents/${testData.coffeeReceipt.id}/content")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should GET document content`(testData: DocumentsApiTestData) {
        testDocumentsStorage.stub {
            onBlocking {
                getDocumentContent(testData.fryWorkspace, "test-location")
            } doReturn "test-content".toDataBuffers()
        }

        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents/${testData.coffeeReceipt.id}/content")
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
    fun `should return 404 if workspace is not found when requesting document content`(testData: DocumentsApiTestData) {
        client.get()
            .uri("/api/workspaces/5634632/documents/${testData.coffeeReceipt.id}/content")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting document content`(
        testData: DocumentsApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents/${testData.coffeeReceipt.id}/content")
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if document belongs to another workspace when requesting document content`(
        testData: DocumentsApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents/${testData.anotherFryWorkspaceDocument.id}/content")
            .verifyNotFound("Document ${testData.anotherFryWorkspaceDocument.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating document`(testData: DocumentsApiTestData) {
        client.post()
            .uri("/api/workspaces/995943/documents")
            .bodyValue(testData.createDefaultFileToUpload().build())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating document`(testData: DocumentsApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents")
            .bodyValue(testData.createDefaultFileToUpload().build())
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should upload a new file and invoke documents storage`(testData: DocumentsApiTestData) {
        mockDocumentsStorage(testData)

        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents")
            .bodyValue(testData.createDefaultFileToUpload().build())
            .verifyOkAndJsonBody {
                node("name").isString.isEqualTo("test-file.txt")
                node("id").isNumber.isNotNull
                node("version").isNumber.isEqualTo(BigDecimal.ZERO)
                node("timeUploaded").isString.isEqualTo(MOCK_TIME_VALUE)
                node("sizeInBytes").isNumber.isEqualTo(42.toBigDecimal())
            }
    }

    private fun mockDocumentsStorage(testData: DocumentsApiTestData) {
        testDocumentsStorage.mock.stub {
            onBlocking {
                saveDocument(argThat { workspace == testData.fryWorkspace && fileName == "test-file.txt" })
            } doReturn SaveDocumentResponse(
                storageLocation = "test-location",
                sizeInBytes = 42
            )
        }
    }

    @Test
    @WithMockFryUser
    fun `should filter documents by ids`(testData: DocumentsApiTestData) {
        client.get()
            .uri { builder ->
                builder.replacePath("/api/workspaces/${testData.fryWorkspace.id}/documents")
                    .queryParam("id[eq]", "${testData.cheesePizzaAndALargeSodaReceipt.id}")
                    .build()
            }
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("1")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                        "name": "unknown",
                        "id": ${testData.cheesePizzaAndALargeSodaReceipt.id},
                        "version": 0,
                        "timeUploaded": "$MOCK_TIME_VALUE",
                        "sizeInBytes": null
                    }"""
                    )
                )
            }
    }

    @Test
    fun `should allow GET access for document download token only for logged in user`(testData: DocumentsApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents/${testData.coffeeReceipt.id}/download-token")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting document download token`(testData: DocumentsApiTestData) {
        client.get()
            .uri("/api/workspaces/5634632/documents/${testData.coffeeReceipt.id}/download-token")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting document download token`(
        testData: DocumentsApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents/${testData.coffeeReceipt.id}/download-token")
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if document belongs to another workspace when requesting document download token`(
        testData: DocumentsApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents/${testData.anotherFryWorkspaceDocument.id}/download-token")
            .verifyNotFound("Document ${testData.anotherFryWorkspaceDocument.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return document download token`(
        testData: DocumentsApiTestData
    ) {
        downloadsService.stub {
            onBlocking {
                createDownloadToken<DocumentDownloadMetadata>(
                    argThat { this is DocumentsService },
                    argThat { this.documentId == testData.coffeeReceipt.id }
                )
            } doReturn "token"
        }

        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents/${testData.coffeeReceipt.id}/download-token")
            .verifyOkAndJsonBody {
                node("token").isString.isEqualTo("token")
            }
    }

    class DocumentsApiTestData : TestData {
        val fry = Prototypes.platformUser(userName = "Fry", documentsStorage = "test-storage")
        val fryWorkspace = Prototypes.workspace(owner = fry)
        val anotherFryWorkspace = Prototypes.workspace(owner = fry)
        val anotherFryWorkspaceDocument = Prototypes.document(workspace = anotherFryWorkspace)
        val farnsworth = Prototypes.farnsworth()
        val coffeeReceipt = Prototypes.document(
            name = "100_cups.pdf",
            workspace = fryWorkspace,
            storageId = "test-storage",
            storageLocation = "test-location",
            timeUploaded = MOCK_TIME,
            sizeInBytes = 42
        )
        val cheesePizzaAndALargeSodaReceipt = Prototypes.document(
            name = "unknown",
            workspace = fryWorkspace,
            timeUploaded = MOCK_TIME,
            sizeInBytes = null
        )

        override fun generateData() = listOf(
            fry, fryWorkspace, coffeeReceipt, cheesePizzaAndALargeSodaReceipt,
            farnsworth,
            anotherFryWorkspace, anotherFryWorkspaceDocument
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

    class TestDocumentsStorage(
        val mock: DocumentsStorage = mock(DocumentsStorage::class.java)
    ) : DocumentsStorage by mock

    @TestConfiguration
    class DocumentControllerTestConfig {
        @Bean
        fun testDocumentsStorage() = TestDocumentsStorage()
    }
}
