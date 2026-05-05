package io.orangebuffalo.simpleaccounting.business.api.documents

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.documents.DocumentDownloadMetadata
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadsService
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ContentDisposition
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.nio.charset.StandardCharsets

@DisplayName("Documents Content API")
class DocumentsContentApiTest(
    @Autowired private val client: WebTestClient,
    @Autowired private val downloadsService: DownloadsService,
    @Autowired private val documentsService: DocumentsService,
    @Autowired private val testDocumentsStorage: TestDocumentsStorage,
) : SaIntegrationTestBase() {

    private val documentContentText = "Good news, everyone! Delivery receipt"
    private val documentContent = documentContentText.toByteArray(StandardCharsets.UTF_8)

    @BeforeEach
    fun resetStorage() {
        testDocumentsStorage.reset()
    }

    @Test
    fun `should allow GET access to downloads without authentication`() {
        val token = createDownloadToken()

        client.get()
            .uri("/api/documents/download/$token")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `should return 404 when token is not found`() {
        client.get()
            .uri("/api/documents/download/unknown-token")
            .verifyNotFound("Token unknown-token is not found")
    }

    @Test
    fun `should GET content by token`() {
        val token = createDownloadToken()

        client.get()
            .uri("/api/documents/download/$token")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentDisposition(
                ContentDisposition.parse("attachment; filename=\"delivery-receipt.pdf\"")
            )
            .expectHeader().contentLength(documentContent.size.toLong())
            .expectHeader().contentType(MediaType.APPLICATION_PDF)
            .expectBody()
            .consumeWith { exchange ->
                exchange.responseBody.shouldNotBeNull().also { body ->
                    val text = String(body, StandardCharsets.UTF_8)
                    text.shouldBe(documentContentText)
                }
            }
    }

    private fun createDownloadToken(): String {
        testDocumentsStorage.mockDocumentContent("planet-express-receipt-location", documentContent)

        return runBlocking {
            runAs(preconditions.fry.toSecurityPrincipal()) {
                downloadsService.createDownloadToken(
                    documentsService,
                    DocumentDownloadMetadata(preconditions.document.id!!)
                )
            }
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(
                userName = "Fry",
                documentsStorage = TestDocumentsStorage.STORAGE_ID,
            )
            val document = document(
                workspace = workspace(owner = fry),
                name = "delivery-receipt.pdf",
                storageId = TestDocumentsStorage.STORAGE_ID,
                storageLocation = "planet-express-receipt-location",
                sizeInBytes = documentContent.size.toLong(),
                mimeType = "application/pdf",
            )
        }
    }
}
