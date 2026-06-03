package io.orangebuffalo.simpleaccounting.infra.graphql

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.expectThatJsonBody
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlRawQuery
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlRawQueryWithVariables
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Integration tests for [SaGraphQLNullFieldValidationInstrumentation].
 *
 * Verifies that all three ways a client can omit or null a non-nullable argument are uniformly
 * transformed into a [io.orangebuffalo.simpleaccounting.business.api.errors.SaGrapQlErrorType.FIELD_VALIDATION_FAILURE]
 * response:
 * 1. Inline `null` literal in the query document (e.g. `rateInBps: null`).
 * 2. Absent field in the query document (e.g. omitting `rateInBps` from the argument list).
 * 3. Typed GraphQL variable declared as non-null but supplied as `null` in the variables map
 *    (e.g. `$rateInBps: Int!` with `{"rateInBps": null}`).
 *
 * Cases 1 and 2 produce a [graphql.validation.ValidationError]. Case 3 is handled by
     * pre-coercion validation so it can report all variable violations before graphql-java's
     * fail-fast coercion path runs.
 */
@DisplayName("SaGraphQLNullFieldValidationInstrumentation")
class SaGraphQLNullFieldValidationInstrumentationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }

    @Nested
    @DisplayName("Inline null literal in query document")
    inner class InlineNullLiteral {

        @Test
        fun `should transform inline null for non-null field into FIELD_VALIDATION_FAILURE`() {
            client.graphqlRawQuery(
                """
                mutation {
                  createGeneralTax(workspaceId: "${preconditions.workspace.id}", title: "VAT", rateInBps: null) {
                    rateInBps
                  }
                }
                """.trimIndent()
            )
                .from(preconditions.fry)
                .executeAndVerifyValidationError(
                    violationPath = "rateInBps",
                    error = "MustNotBeNull",
                    message = "must not be null",
                    path = DgsConstants.MUTATION.CreateGeneralTax,
                    locationLine = 2,
                    locationColumn = 3,
                )
        }
    }

    @Nested
    @DisplayName("Absent field in query document")
    inner class AbsentField {

        @Test
        fun `should transform absent non-null field into FIELD_VALIDATION_FAILURE`() {
            client.graphqlRawQuery(
                """
                mutation {
                  createGeneralTax(workspaceId: "${preconditions.workspace.id}", title: "VAT") {
                    rateInBps
                  }
                }
                """.trimIndent()
            )
                .from(preconditions.fry)
                .executeAndVerifyValidationError(
                    violationPath = "rateInBps",
                    error = "MustNotBeNull",
                    message = "must not be null",
                    path = DgsConstants.MUTATION.CreateGeneralTax,
                    locationLine = 2,
                    locationColumn = 3,
                )
        }
    }

    @Nested
    @DisplayName("Null value passed via typed GraphQL variable")
    inner class NullVariable {

        @Test
        fun `should transform null top-level variable for non-null type into FIELD_VALIDATION_FAILURE`() {
            // The variable $rateInBps is declared as Int! (non-null) but null is supplied in the
            // variables map. Pre-coercion validation must transform it the same way as inline
            // null literals, before graphql-java's fail-fast variable coercion runs.
            client.graphqlRawQueryWithVariables(
                query = """
                mutation(${'$'}rateInBps: Int!) {
                  createGeneralTax(workspaceId: "${preconditions.workspace.id}", title: "VAT", rateInBps: ${'$'}rateInBps) {
                    rateInBps
                  }
                }
                """.trimIndent(),
                variables = buildJsonObject { put("rateInBps", JsonNull) },
            )
                .from(preconditions.fry)
                .executeAndVerifyValidationError(
                    violationPath = "rateInBps",
                    error = "MustNotBeNull",
                    message = "must not be null",
                    path = DgsConstants.MUTATION.CreateGeneralTax,
                    locationLine = 1,
                    locationColumn = 10,
                )
        }

        @Test
        fun `should report all null non-null argument variables`() {
            client.graphqlRawQueryWithVariables(
                query = """
                mutation(${'$'}name: String!, ${'$'}defaultCurrency: String!) {
                  createWorkspace(name: ${'$'}name, defaultCurrency: ${'$'}defaultCurrency) {
                    id
                  }
                }
                """.trimIndent(),
                variables = buildJsonObject {
                    put("name", JsonNull)
                    put("defaultCurrency", JsonNull)
                },
            )
                .from(preconditions.fry)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    val response = Json.parseToJsonElement(this).jsonObject
                    val error = response["errors"]!!.jsonArray.single().jsonObject
                    error["message"]!!.jsonPrimitive.content.shouldBe("Validation failed")
                    error["extensions"]!!.jsonObject["errorType"]!!.jsonPrimitive.content
                        .shouldBe("FIELD_VALIDATION_FAILURE")

                    val validationErrors = error["extensions"]!!.jsonObject["validationErrors"]!!
                        .jsonArray
                        .map { it.jsonObject }
                    withClue("Should report all null fields in one response") {
                        validationErrors.map { it["path"]!!.jsonPrimitive.content }
                            .shouldContainExactly("name", "defaultCurrency")
                    }
                    validationErrors.forEach { validationError ->
                        validationError["error"]!!.jsonPrimitive.content.shouldBe("MustNotBeBlank")
                        validationError["message"]!!.jsonPrimitive.content.shouldBe("must not be blank")
                    }
                }
        }

        @Test
        fun `should report nullability and directive validation errors together`() {
            client.graphqlRawQueryWithVariables(
                query = """
                mutation(${'$'}name: String!, ${'$'}defaultCurrency: String!) {
                  createWorkspace(name: ${'$'}name, defaultCurrency: ${'$'}defaultCurrency) {
                    id
                  }
                }
                """.trimIndent(),
                variables = buildJsonObject {
                    put("name", JsonNull)
                    put("defaultCurrency", "")
                },
            )
                .from(preconditions.fry)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    val response = Json.parseToJsonElement(this).jsonObject
                    val error = response["errors"]!!.jsonArray.single().jsonObject
                    error["message"]!!.jsonPrimitive.content.shouldBe("Validation failed")
                    error["extensions"]!!.jsonObject["errorType"]!!.jsonPrimitive.content
                        .shouldBe("FIELD_VALIDATION_FAILURE")

                    val validationErrors = error["extensions"]!!.jsonObject["validationErrors"]!!
                        .jsonArray
                        .map { it.jsonObject }
                    validationErrors.map {
                        it["path"]!!.jsonPrimitive.content to it["error"]!!.jsonPrimitive.content
                    }.shouldContainExactly(
                        "name" to "MustNotBeBlank",
                        "defaultCurrency" to "MustNotBeBlank",
                    )
                }
        }

        @Test
        fun `should report size validation errors for variables`() {
            client.graphqlRawQueryWithVariables(
                query = """
                mutation(${'$'}name: String!, ${'$'}defaultCurrency: String!) {
                  createWorkspace(name: ${'$'}name, defaultCurrency: ${'$'}defaultCurrency) {
                    id
                  }
                }
                """.trimIndent(),
                variables = buildJsonObject {
                    put("name", "Planet Express")
                    put("defaultCurrency", "USDD")
                },
            )
                .from(preconditions.fry)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    val response = Json.parseToJsonElement(this).jsonObject
                    val validationError = response["errors"]!!
                        .jsonArray.single().jsonObject["extensions"]!!.jsonObject["validationErrors"]!!
                        .jsonArray.single().jsonObject

                    validationError["path"]!!.jsonPrimitive.content.shouldBe("defaultCurrency")
                    validationError["error"]!!.jsonPrimitive.content.shouldBe("SizeConstraintViolated")
                    validationError["message"]!!.jsonPrimitive.content.shouldBe("size must be between 0 and 3")
                }
        }

        @Test
        fun `should accept false boolean literal for required field`() {
            client.graphqlRawQuery(
                """
                mutation {
                  createCategory(workspaceId: "${preconditions.workspace.id}", name: "Robot oil", income: false, expense: true) {
                    income
                    expense
                  }
                }
                """.trimIndent()
            )
                .from(preconditions.fry)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    val response = Json.parseToJsonElement(this).jsonObject
                    response["errors"].shouldBe(null)
                    val category = response["data"]!!.jsonObject["createCategory"]!!.jsonObject
                    category["income"]!!.jsonPrimitive.content.shouldBe("false")
                    category["expense"]!!.jsonPrimitive.content.shouldBe("true")
                }
        }
    }
}
