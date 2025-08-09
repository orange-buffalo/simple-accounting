package io.orangebuffalo.simpleaccounting.infra.graphql

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.tests.infra.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.environment.TestConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import java.nio.file.Files
import java.nio.file.Path

private const val COMMITTED_SCHEMA_PATH = "src/test/resources/api-schema.graphqls"

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
                if (TestConfig.instance.apiContracts.overrideCommittedSchema) {
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
