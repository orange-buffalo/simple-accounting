package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Fry
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.DocumentRepository
import io.orangebuffalo.accounting.simpleaccounting.web.DbHelper
import io.orangebuffalo.accounting.simpleaccounting.web.MOCK_TIME_VALUE
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import io.orangebuffalo.accounting.simpleaccounting.web.mockCurrentTime
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.util.InMemoryResource
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.TestPropertySourceUtils
import org.springframework.test.web.reactive.server.WebTestClient
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = [DocumentApiControllerIT.TempDirectoryInitializer::class])
@DisplayName("Document API ")
class DocumentApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val documentRepository: DocumentRepository,
    @Autowired val dbHelper: DbHelper,
    @Value("\${simpleaccounting.documents.storage.local-fs.base-directory}") val baseDocumentsDirectory: Path
) {

    @MockBean
    lateinit var timeServiceMock: TimeService

    @Test
    @WithMockUser(username = "Fry")
    fun `should upload a new file and store it in a local file system`(fry: Fry) {
        val documentId = dbHelper.getNextId()
        mockCurrentTime(timeServiceMock)

        val multipartBodyBuilder = MultipartBodyBuilder()
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
            .apply {
                part("notes", "Shut up and take my money")
            }

        client.post()
            .uri("/api/v1/user/workspaces/${fry.workspace.id}/documents")
            .syncBody(multipartBodyBuilder.build())
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                node("name").isString.isEqualTo("test-file.txt")
                node("id").isNumber.isEqualTo(BigDecimal.valueOf(documentId))
                node("version").isNumber.isEqualTo(BigDecimal.ZERO)
                node("timeUploaded").isString.isEqualTo(MOCK_TIME_VALUE)
                node("notes").isString.isEqualTo("Shut up and take my money")
            }

        val document = documentRepository.findById(documentId)
        assertThat(document).isPresent.hasValueSatisfying {
            assertThat(it.workspace).isEqualTo(fry.workspace)

            val documentFile = File(baseDocumentsDirectory.toFile(), it.storageProviderLocation)
            assertThat(documentFile).isFile().exists().hasContent("test-content")
        }
    }

    @Test
    @WithMockUser(username = "Fry")
    fun `should return documents by ids`(fry: Fry) {
        client.get()
            .uri { builder ->
                builder.replacePath("/api/v1/user/workspaces/${fry.workspace.id}/documents")
                    .queryParam("id[eq]", "${fry.cheesePizzaAndALargeSodaReceipt.id}")
                    .queryParam("id[eq]", "${fry.coffeeReceipt.id}")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                        "name": "unknown",
                        "id": ${fry.cheesePizzaAndALargeSodaReceipt.id},
                        "version": 0,
                        "timeUploaded": "$MOCK_TIME_VALUE",
                        "notes": "Panucci's Pizza"
                    }"""
                    ),

                    json(
                        """{
                        "name": "100_cups.pdf",
                        "id": ${fry.coffeeReceipt.id},
                        "version": 0,
                        "timeUploaded": "$MOCK_TIME_VALUE",
                        "notes": null
                    }"""
                    )
                )
            }
    }

    class TempDirectoryInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            val tempDirectory = Files.createTempDirectory("simple-accounting-test-")
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "simpleaccounting.documents.storage.local-fs.base-directory=" + tempDirectory.toString()
            )
        }
    }
}