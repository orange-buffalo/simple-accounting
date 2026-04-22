package io.orangebuffalo.simpleaccounting.infra.graphql

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlRawQuery
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlRawQueryWithVariables
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
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
 * Cases 1 and 2 produce a [graphql.validation.ValidationError] and already carry the mutation
 * field path. Case 3 produces a [graphql.execution.NonNullableValueCoercedAsNullException] during
 * variable coercion, which does not carry a mutation path, so the transformed error's `path`
 * is an empty array.
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
                  createGeneralTax(workspaceId: ${preconditions.workspace.id}, title: "VAT", rateInBps: null) {
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
                  createGeneralTax(workspaceId: ${preconditions.workspace.id}, title: "VAT") {
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
            // variables map. graphql-java raises NonNullableValueCoercedAsNullException during
            // variable coercion, which the instrumentation must transform the same way as inline
            // null literals. Because coercion happens before execution, no mutation path is
            // available, so the error's path is an empty array.
            client.graphqlRawQueryWithVariables(
                query = """
                mutation(${'$'}rateInBps: Int!) {
                  createGeneralTax(workspaceId: ${preconditions.workspace.id}, title: "VAT", rateInBps: ${'$'}rateInBps) {
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
                    paths = emptyList(),
                    locationLine = 1,
                    locationColumn = 10,
                )
        }
    }
}
