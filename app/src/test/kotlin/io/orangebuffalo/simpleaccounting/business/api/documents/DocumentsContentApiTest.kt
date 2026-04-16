package io.orangebuffalo.simpleaccounting.business.api.documents

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadContentResponse
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadsService
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.tests.infra.utils.toDataBuffers
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ContentDisposition
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import java.nio.charset.StandardCharsets

@DisplayName("Documents Content API")
class DocumentsContentApiTest(
    @Autowired val client: WebTestClient
) : SaIntegrationTestBase() {

    @MockitoBean
    lateinit var downloadsService: DownloadsService

    @Test
    fun `should allow GET access to downloads without authentication`() {
        mockDownloadsService()

        client.get()
            .uri("/api/downloads?token=42")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `should return 404 when service throws EntityNotFoundException`() {
        downloadsService.stub {
            onBlocking { getContentByToken("42") } doThrow EntityNotFoundException("Not found")
        }

        client.get()
            .uri("/api/downloads?token=42")
            .verifyNotFound("Not found")
    }

    @Test
    fun `should GET content by token`() {
        mockDownloadsService()

        client.get()
            .uri("/api/downloads?token=42")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentDisposition(ContentDisposition.parse("attachment; filename=\"file.pdf\""))
            .expectHeader().contentLength(77)
            .expectHeader().contentType(MediaType.APPLICATION_PDF)
            .expectBody()
            .consumeWith { exchange ->
                exchange.responseBody.shouldNotBeNull().also { body ->
                    val text = String(body, StandardCharsets.UTF_8)
                    text.shouldBe("test-content")
                }
            }
    }

    private fun mockDownloadsService() {
        downloadsService.stub {
            onBlocking { getContentByToken("42") } doReturn DownloadContentResponse(
                fileName = "file.pdf",
                content = "test-content".toDataBuffers(),
                sizeInBytes = 77,
                contentType = "application/pdf"
            )
        }
    }
}
