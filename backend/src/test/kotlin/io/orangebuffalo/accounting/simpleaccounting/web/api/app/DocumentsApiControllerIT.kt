package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.DocumentRepository
import io.orangebuffalo.accounting.simpleaccounting.services.storage.DocumentsStorage
import io.orangebuffalo.accounting.simpleaccounting.services.storage.StorageProviderResponse
import io.orangebuffalo.accounting.simpleaccounting.web.*
import kotlinx.coroutines.runBlocking
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.util.InMemoryResource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.StreamUtils
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Documents API ")
class DocumentsApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val documentRepository: DocumentRepository,
    @Autowired val dbHelper: DbHelper,
    @Autowired val testDocumentsStorage: TestDocumentsStorage
) {

    @MockBean
    lateinit var timeServiceMock: TimeService

    @BeforeEach
    fun setup() {
        whenever(testDocumentsStorage.mock.getId()) doReturn "test-storage"
    }

    @Test
    @WithMockUser(username = "Fry")
    fun `should upload a new file and invoke documents storage`(testData: DocumentsApiTestData) {
        mockCurrentTime(timeServiceMock)

        runBlocking {
            whenever(testDocumentsStorage.mock.saveDocument(any(), eq(testData.fryWorkspace)))
                .doReturn(
                    StorageProviderResponse(
                        storageProviderLocation = "test-location",
                        sizeInBytes = 42
                    )
                )
        }

        val documentContent = InMemoryResource("test-content")
        val multipartBodyBuilder = MultipartBodyBuilder()
            .apply {
                part(
                    "file",
                    documentContent,
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
            .apply {
                part("notes", "Shut up and take my money")
            }

        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/documents")
            .syncBody(multipartBodyBuilder.build())
            .verifyOkAndJsonBody {
                node("name").isString.isEqualTo("test-file.txt")
                node("id").isNumber.isNotNull
                node("version").isNumber.isEqualTo(BigDecimal.ZERO)
                node("timeUploaded").isString.isEqualTo(MOCK_TIME_VALUE)
                node("notes").isString.isEqualTo("Shut up and take my money")
                node("sizeInBytes").isNumber.isEqualTo(42.toBigDecimal())
            }
    }

    @Test
    @WithMockUser(username = "Fry")
    fun `should return documents by ids`(testData: DocumentsApiTestData) {
        client.get()
            .uri { builder ->
                builder.replacePath("/api/workspaces/${testData.fryWorkspace.id}/documents")
                    .queryParam("id[eq]", "${testData.cheesePizzaAndALargeSodaReceipt.id}")
                    .queryParam("id[eq]", "${testData.coffeeReceipt.id}")
                    .build()
            }
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
                        "notes": "Panucci's Pizza",
                        "sizeInBytes": null
                    }"""
                    ),

                    json(
                        """{
                        "name": "100_cups.pdf",
                        "id": ${testData.coffeeReceipt.id},
                        "version": 0,
                        "timeUploaded": "$MOCK_TIME_VALUE",
                        "notes": null,
                        "sizeInBytes": 42
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(username = "Fry")
    fun `should download document content`(testData: DocumentsApiTestData) {
        val testContent = InMemoryResource("test-content")

        runBlocking {
            whenever(testDocumentsStorage.mock.getDocumentContent(testData.fryWorkspace, "test-location"))
                .doReturn(
                    DataBufferUtils.read(
                        testContent,
                        DefaultDataBufferFactory(),
                        StreamUtils.BUFFER_SIZE
                    )
                )
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
                assertThat(exchange.responseBody).isNotNull().satisfies { body ->
                    val text = String(body, StandardCharsets.UTF_8)
                    assertThat(text).isEqualTo("test-content")
                }
            }
    }

    class DocumentsApiTestData : TestData {
        val fry = Prototypes.platformUser(userName = "Fry", documentsStorage = "test-storage")
        val fryWorkspace = Prototypes.workspace(owner = fry)
        val coffeeReceipt = Prototypes.document(
            name = "100_cups.pdf",
            workspace = fryWorkspace,
            storageProviderId = "test-storage",
            storageProviderLocation = "test-location",
            timeUploaded = MOCK_TIME,
            sizeInBytes = 42
        )
        val cheesePizzaAndALargeSodaReceipt = Prototypes.document(
            name = "unknown",
            workspace = fryWorkspace,
            timeUploaded = MOCK_TIME,
            notes = "Panucci's Pizza",
            sizeInBytes = null
        )

        override fun generateData() = listOf(
            fry, fryWorkspace, coffeeReceipt, cheesePizzaAndALargeSodaReceipt
        )
    }

    class TestDocumentsStorage(
        val mock: DocumentsStorage = Mockito.mock(DocumentsStorage::class.java)
    ) : DocumentsStorage by mock

    @TestConfiguration
    class DocumentControllerTestConfig {
        @Bean
        fun testDocumentStorageProvider() = TestDocumentsStorage()
    }
}