package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.DocumentRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
import io.orangebuffalo.accounting.simpleaccounting.web.DbHelper
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
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
import org.springframework.transaction.support.TransactionTemplate
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import javax.persistence.EntityManager

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = [DocumentApiControllerIT.TempDirectoryInitializer::class])
@DisplayName("Document API ")
class DocumentApiControllerIT {

    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var workspaceRepo: WorkspaceRepository

    @Autowired
    lateinit var documentRepository: DocumentRepository

    @Autowired
    lateinit var dbHelper: DbHelper

    @Value("\${simpleaccounting.documents.storage.local-fs.base-directory}")
    lateinit var baseDocumentsDirectory: Path

    @Test
    @WithMockUser(username = "Fry")
    fun `should upload a new file and store it in a local file system`() {
        val workspace = workspaceRepo.findAll().first { it.owner.userName == "Fry" }
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
            .uri("/api/v1/user/workspaces/${workspace.id}/documents")
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

        val document = documentRepository.findById(documentId).orElseThrow(::IllegalStateException)
        val documentFile = File(baseDocumentsDirectory.toFile(), document.storageProviderLocation)
        assertThat(documentFile).isFile().exists().hasContent("test-content")
    }

    @TestConfiguration
    class Config {
        @Bean
        fun testSetupRunner(
            transactionTemplate: TransactionTemplate,
            entityManager: EntityManager
        ): ApplicationRunner = ApplicationRunner { _ ->

            transactionTemplate.execute {
                val fry = PlatformUser(
                    userName = "Fry",
                    passwordHash = "qwertyHash",
                    isAdmin = false
                )
                entityManager.persist(fry)

                entityManager.persist(
                    Workspace(
                        name = "fry-files",
                        owner = fry,
                        taxEnabled = false,
                        multiCurrencyEnabled = false,
                        defaultCurrency = "AUD"
                    )
                )
            }
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