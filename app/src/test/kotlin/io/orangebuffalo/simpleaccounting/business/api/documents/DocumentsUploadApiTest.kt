package io.orangebuffalo.simpleaccounting.business.api.documents

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integration.uploads.UploadsRepository
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBodyEqualTo
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME_VALUE
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.util.InMemoryResource
import org.springframework.test.web.reactive.server.WebTestClient

@DisplayName("Documents Upload API")
class DocumentsUploadApiTest(
    @Autowired private val client: WebTestClient,
    @Autowired private val uploadsRepository: UploadsRepository,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("POST /api/documents/upload/{token}")
    inner class UploadDocument {

        @Test
        fun `should allow POST access to upload without authentication`() {
            val token = createUploadToken()

            client.post()
                .uri("/api/documents/upload/$token")
                .bodyValue(createDefaultFileToUpload().build())
                .exchange()
                .expectStatus().isOk
        }

        @Test
        fun `should return 404 when token is not found`() {
            client.post()
                .uri("/api/documents/upload/invalid-token")
                .bodyValue(createDefaultFileToUpload().build())
                .verifyNotFound("Token invalid-token is not found")
        }

        @Test
        fun `should upload a document and return GQL Document DTO`() {
            val token = createUploadToken()

            client.post()
                .uri("/api/documents/upload/$token")
                .bodyValue(createDefaultFileToUpload().build())
                .verifyOkAndJsonBodyEqualTo {
                    put("id", "${JsonValues.ANY_NUMBER}")
                    put("version", 0)
                    put("name", "test-file.txt")
                    put("timeUploaded", MOCK_TIME_VALUE)
                    put("sizeInBytes", 12)
                    put("storageId", "test-storage")
                    put("mimeType", "text/plain")
                    putJsonArray("usedBy") {}
                }
        }

        @Test
        fun `should upload a document with binary content type`() {
            val token = createUploadToken()

            val body = MultipartBodyBuilder()
                .apply {
                    part(
                        "file",
                        InMemoryResource("binary-content"),
                        MediaType.APPLICATION_OCTET_STREAM,
                    ).header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                            .builder("form-data")
                            .name("file")
                            .filename("binary-data.bin")
                            .build().toString()
                    )
                }

            client.post()
                .uri("/api/documents/upload/$token")
                .bodyValue(body.build())
                .verifyOkAndJsonBodyEqualTo {
                    put("id", "${JsonValues.ANY_NUMBER}")
                    put("version", 0)
                    put("name", "binary-data.bin")
                    put("timeUploaded", MOCK_TIME_VALUE)
                    put("sizeInBytes", 14)
                    put("storageId", "test-storage")
                    put("mimeType", "application/octet-stream")
                    putJsonArray("usedBy") {}
                }
        }

        @Test
        fun `should store document in correct workspace`() {
            val token = createUploadToken()

            client.post()
                .uri("/api/documents/upload/$token")
                .bodyValue(createDefaultFileToUpload().build())
                .exchange()
                .expectStatus().isOk

            val documents = aggregateTemplate.findAll(io.orangebuffalo.simpleaccounting.business.documents.Document::class.java)
            documents.shouldHaveSize(1)
            documents.first().should {
                it.workspaceId.shouldBe(preconditions.fryWorkspace.id)
                it.name.shouldBe("test-file.txt")
                it.storageId.shouldBe("test-storage")
                it.mimeType.shouldBe("text/plain")
                it.sizeInBytes.shouldBe(12)
            }
        }
    }

    private var tokenCounter = 0

    private fun createUploadToken(): String {
        val token = "test-upload-token-${++tokenCounter}"
        runBlocking {
            uploadsRepository.storeUploadRequest(
                token = token,
                workspaceId = preconditions.fryWorkspace.id!!,
                userName = preconditions.fry.userName,
            )
        }
        return token
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
            val fryWorkspace = workspace(owner = fry)
        }
    }

    private fun createDefaultFileToUpload(): MultipartBodyBuilder = MultipartBodyBuilder()
        .apply {
            part(
                "file",
                InMemoryResource("test-content"),
                MediaType.TEXT_PLAIN,
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
