package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Fry
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.DocumentRepository
import io.orangebuffalo.accounting.simpleaccounting.web.DbHelper
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
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

    @Test
    @WithMockUser(username = "Fry")
    fun `should upload a new file and store it in a local file system`(fry: Fry) {
        val documentId = dbHelper.getNextId()

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
                // todo this makes sense to rewrite with a mocked time service
                node("dateUploaded").isString
                    .matches("""[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]{1,3})?[+-][0-9]{2}:[0-9]{2}""")
                node("notes").isString.isEqualTo("Shut up and take my money")
            }

        val document = documentRepository.findById(documentId)
        assertThat(document).isPresent.hasValueSatisfying {
            assertThat(it.workspace).isEqualTo(fry.workspace)

            val documentFile = File(baseDocumentsDirectory.toFile(), it.storageProviderLocation)
            assertThat(documentFile).isFile().exists().hasContent("test-content")
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