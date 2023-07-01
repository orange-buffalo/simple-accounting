package io.orangebuffalo.simpleaccounting.web.api

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.stub
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.downloads.DownloadContentResponse
import io.orangebuffalo.simpleaccounting.services.integration.downloads.DownloadsService
import io.orangebuffalo.simpleaccounting.infra.utils.toDataBuffers
import io.orangebuffalo.simpleaccounting.infra.api.verifyNotFound
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.ContentDisposition
import org.springframework.test.web.reactive.server.WebTestClient
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

@SimpleAccountingIntegrationTest
@DisplayName("Downloads API ")
class DownloadsApiControllerIT(
    @Autowired val client: WebTestClient
) {

    @MockBean
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

    private fun mockDownloadsService() {
        downloadsService.stub {
            onBlocking { getContentByToken("42") } doReturn DownloadContentResponse(
                fileName = "file.pdf",
                content = "test-content".toDataBuffers(),
                sizeInBytes = 77
            )
        }
    }
}
