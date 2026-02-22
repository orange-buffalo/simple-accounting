package io.orangebuffalo.simpleaccounting.infra.graphql

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.environment.TestConfig
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import java.nio.file.Files
import java.nio.file.Path

private const val COMMITTED_SCHEMA_PATH = "src/test/resources/api-schema.graphqls"
private val logger = KotlinLogging.logger {}

class GraphqlSchemaTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Test
    fun `should have API schema up to date`() {
        client.get()
            .uri("/api/graphql/schema")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>().consumeWith { body ->
                val actualSchema = body.responseBody.shouldNotBeNull()
                val shouldOverrideSchema = System.getProperty("simpleaccounting.graphql.updateSchema")?.toBoolean()
                        ?: TestConfig.instance.apiContracts.overrideCommittedSchema

                if (shouldOverrideSchema) {
                    logger.info { "Overriding GraphQL schema at $COMMITTED_SCHEMA_PATH" }
                    Files.writeString(
                        Path.of(COMMITTED_SCHEMA_PATH),
                        actualSchema,
                    )
                } else {
                    val committedSchema = Files.readString(Path.of(COMMITTED_SCHEMA_PATH))
                    actualSchema.shouldBe(committedSchema)
                }
            }
    }
}
