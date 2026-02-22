package io.orangebuffalo.simpleaccounting.infra.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.flipkart.zjsonpatch.JsonDiff
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import io.orangebuffalo.simpleaccounting.infra.ui.SpaWebFilter
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.nio.file.Files
import java.time.Duration

private val logger = KotlinLogging.logger {}

@TestPropertySource(
    properties = [
        "springdoc.api-docs.path=/api-docs",
        "springdoc.default-produces-media-type=application/json",
        "springdoc.remove-broken-reference-definitions=false",
    ]
)
@DisplayName("API Spec")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ApiSpecTest(
    @Autowired val client: WebTestClient
) : SaIntegrationTestBase() {

    @MockitoBean
    private lateinit var spaWebFilter: SpaWebFilter

    @Test
    @Order(1)
    fun `should be up-to-date in VCS`() {
        // disable SPA - otherwise docs are not rendered
        whenever(spaWebFilter.filter(any(), any())) doAnswer {
            val webFilterChain = it.arguments[1] as WebFilterChain
            val serverWebExchange = it.arguments[0] as ServerWebExchange
            webFilterChain.filter(serverWebExchange)
        }

        val committedSpec = File("src/test/resources/api-spec.yaml")

        client
            .mutate()
            // default timeout of 5s is not enough on CI sometimes (concurrent JVMs running test, limited CPU resource)
            .responseTimeout(Duration.ofSeconds(10))
            .build()
            .get()
            .uri("/api-docs.yaml")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .consumeWith {
                val currentApiSpec = it.responseBody!!

                val mapper = ObjectMapper(YAMLFactory())
                val schemaDiff = JsonDiff.asJson(
                    mapper.readTree(committedSpec.inputStream()),
                    mapper.readTree(currentApiSpec)
                )

                if (shouldOverrideCommittedFiles()) {
                    logger.warn { "Overriding committed spec" }
                    committedSpec.writeText(currentApiSpec)
                }

                assertThat(schemaDiff).isInstanceOfSatisfying(ArrayNode::class.java) { changes ->
                    assertThat(changes.size())
                        .withFailMessage { schemaDiff.toPrettyString() }
                        .isZero()
                }
            }
    }

    @Test
    @Order(2)
    fun `should have TS API client up to date`() {
        val baseCommittedDirectory = File("../frontend/src/services/api/generated")
        val committedFiles = getRelativeFilePathsByBaseDir(baseCommittedDirectory)

        val tmpDir = Files.createTempDirectory("api-spec-test").toFile()
        val generator = OpenApiGenerator()
            .withFileSystemBind("src/test/resources/api-spec.yaml", "/api-spec.yaml", BindMode.READ_ONLY)
            .withFileSystemBind(tmpDir.absolutePath, "/out", BindMode.READ_WRITE)
            .withCommand(
                "generate", "-g", "typescript-fetch", "-o", "/out", "-i", "/api-spec.yaml",
                "--type-mappings=Date=string"
            )
            .withLogConsumer { logger.info { it.utf8String } }
            .waitingFor(Wait.forLogMessage(".*Thanks for using OpenAPI Generator.*", 1))
        generator.start()

        val generatedFiles = getRelativeFilePathsByBaseDir(tmpDir).toMutableSet()
        generatedFiles.remove(".openapi-generator-ignore")
        generatedFiles.remove(".openapi-generator/FILES")
        generatedFiles.remove(".openapi-generator/VERSION")

        assertSoftly { softly ->
            val deletedFiles = committedFiles.subtract(generatedFiles)
            val addedFiles = generatedFiles.subtract(committedFiles)
            val existingFiles = generatedFiles.intersect(committedFiles)

            softly.assertThat(deletedFiles)
                .`as`("Some files are no longer generated")
                .isEmpty()

            softly.assertThat(addedFiles)
                .`as`("New files have been generated")
                .isEmpty()

            val changedFiles = mutableMapOf<String, String>()
            existingFiles.forEach { relativePath ->
                val committedContent = File(baseCommittedDirectory, relativePath).readText()
                val generatedContent = File(tmpDir, relativePath).readText()
                if (committedContent != generatedContent) {
                    changedFiles[relativePath] = generatedContent
                }
                softly.assertThat(committedContent)
                    .`as`("$relativePath has different content")
                    .isEqualTo(generatedContent)
            }

            if (shouldOverrideCommittedFiles()) {
                logger.warn { "Overriding committed client" }

                deletedFiles.forEach { relativePath ->
                    logger.info { "Deleting $relativePath" }
                    File(baseCommittedDirectory, relativePath).delete()
                }

                addedFiles.forEach { relativePath ->
                    logger.info { "Adding $relativePath" }
                    val content = File(tmpDir, relativePath).readText()
                    File(baseCommittedDirectory, relativePath).writeText(content)
                }

                changedFiles.forEach { (relativePath, content) ->
                    logger.info { "Updating $relativePath" }
                    File(baseCommittedDirectory, relativePath).writeText(content)
                }
            }
        }
    }

    private fun getRelativeFilePathsByBaseDir(baseDir: File) = baseDir.walk()
        .filter { it.isFile }
        .map {
            it.relativeTo(baseDir).path
        }
        .toSet()

    private fun shouldOverrideCommittedFiles(): Boolean = System.getenv("OVERRIDE_COMMITTED_FILES") != null

    private class OpenApiGenerator
        : GenericContainer<OpenApiGenerator>("openapitools/openapi-generator-cli:v7.6.0")

}
