package io.orangebuffalo.simpleaccounting.infra.utils

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import java.util.*
import kotlin.NotImplementedError
import kotlin.String
import kotlin.let
import kotlin.to

/**
 * See [ApiRequestsValidationsTestBase.ApiRequestsValidationsArgumentsProvider].
 */
class ApiRequestsValidationsArgumentsProviderTest {

    @Test
    fun `should generate proper arguments for api requests validations`() {
        val testInstance = object : ApiRequestsValidationsTestBase() {
            override val requestExecutionSpec: ApiRequestsValidationsTestRequestExecutionSpec = {
                throw NotImplementedError("Not relevant for this test")
            }

            override val requestBodySpec: ApiRequestsBodyConfiguration = {
                string("mandatoryStringA", mandatory = true, maxLength = 10)
                string("optionalStringA", mandatory = false, minLength = 15, maxLength = 20)
                string("optionalStringMinMaxEqual", mandatory = false, minLength = 13, maxLength = 13)
                nested("emptyNested", mandatory = false) {
                    // intentionally left empty
                }
                nested("optionalNested", mandatory = false) {
                    string("nestedStringA", mandatory = false, maxLength = 2)
                }
                nested("mandatoryNested", mandatory = true) {
                    string("nestedStringB", mandatory = false, maxLength = 5)
                    nested("deeplyNested", mandatory = false) {
                        string("deeplyNestedStringA", mandatory = false, maxLength = 3)
                    }
                }
            }
        }

        val argumentsProvider = ApiRequestsValidationsTestBase.ApiRequestsValidationsArgumentsProvider()
        val extensionsContext = mock<ExtensionContext> {
            whenever(mock.testInstance) doReturn Optional.of(testInstance)
        }
        argumentsProvider.provideArguments(extensionsContext)
            .map { it.toExpectedResult() }
            .toList()
            .shouldContainExactlyInAnyOrder(
                ExpectedResult(
                    description = "Sanity check: all values at their default should succeed",
                    requestBody = expectedRequest(),
                    expectedValidationErrorResponse = null
                ),
                ExpectedResult(
                    description = "mandatoryStringA: should fail when null",
                    requestBody = expectedRequest(
                        mandatoryStringA = JsonNull
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "mandatoryStringA",
                        error = "MustNotBeBlank",
                        message = "must not be blank",
                    )
                ),
                ExpectedResult(
                    description = "mandatoryStringA: should fail when not provided",
                    requestBody = expectedRequest(
                        mandatoryStringA = null
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "mandatoryStringA",
                        error = "MustNotBeBlank",
                        message = "must not be blank",
                    )
                ),
                ExpectedResult(
                    description = "mandatoryStringA: should fail when empty string",
                    requestBody = expectedRequest(
                        mandatoryStringA = JsonPrimitive("")
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "mandatoryStringA",
                        error = "MustNotBeBlank",
                        message = "must not be blank",
                    )
                ),
                ExpectedResult(
                    description = "mandatoryStringA: should fail when blank string",
                    requestBody = expectedRequest(
                        mandatoryStringA = JsonPrimitive(" ")
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "mandatoryStringA",
                        error = "MustNotBeBlank",
                        message = "must not be blank",
                    )
                ),
                ExpectedResult(
                    description = "mandatoryStringA: should fail when longer than 10",
                    requestBody = expectedRequest(
                        mandatoryStringA = JsonPrimitive("a".repeat(11))
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "mandatoryStringA",
                        error = "SizeConstraintViolated",
                        message = "must be between 0 and 10",
                        params = mapOf("min" to "0", "max" to "10")
                    )
                ),
                ExpectedResult(
                    description = "mandatoryStringA: should pass when exactly 10 characters",
                    requestBody = expectedRequest(
                        mandatoryStringA = JsonPrimitive("a".repeat(10))
                    ),
                    expectedValidationErrorResponse = null
                ),
                ExpectedResult(
                    description = "optionalStringA: should fail when longer than 20",
                    requestBody = expectedRequest(
                        optionalStringA = "a".repeat(21)
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "optionalStringA",
                        error = "SizeConstraintViolated",
                        message = "must be between 15 and 20",
                        params = mapOf("min" to "15", "max" to "20")
                    )
                ),
                ExpectedResult(
                    description = "optionalStringA: should pass when exactly 20 characters",
                    requestBody = expectedRequest(
                        optionalStringA = "a".repeat(20)
                    ),
                    expectedValidationErrorResponse = null
                ),
                ExpectedResult(
                    description = "optionalStringA: should fail when shorter than 15",
                    requestBody = expectedRequest(
                        optionalStringA = "a".repeat(14)
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "optionalStringA",
                        error = "SizeConstraintViolated",
                        message = "must be between 15 and 20",
                        params = mapOf("min" to "15", "max" to "20")
                    )
                ),
                 ExpectedResult(
                    description = "optionalStringA: should pass when exactly 15 characters",
                    requestBody = expectedRequest(
                        optionalStringA = "a".repeat(15)
                    ),
                    expectedValidationErrorResponse = null
                ),
                ExpectedResult(
                    description = "optionalStringMinMaxEqual: should fail when longer than 13",
                    requestBody = expectedRequest(
                        optionalStringMinMaxEqual = "a".repeat(14)
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "optionalStringMinMaxEqual",
                        error = "SizeConstraintViolated",
                        message = "must be between 13 and 13",
                        params = mapOf("min" to "13", "max" to "13")
                    )
                ),
                ExpectedResult(
                    description = "optionalStringMinMaxEqual: should fail when shorter than 13",
                    requestBody = expectedRequest(
                        optionalStringMinMaxEqual = "a".repeat(12)
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "optionalStringMinMaxEqual",
                        error = "SizeConstraintViolated",
                        message = "must be between 13 and 13",
                        params = mapOf("min" to "13", "max" to "13")
                    )
                ),
                ExpectedResult(
                    description = "optionalStringMinMaxEqual: should pass when exactly 13 characters",
                    requestBody = expectedRequest(
                        optionalStringMinMaxEqual = "a".repeat(13)
                    ),
                    expectedValidationErrorResponse = null
                ),
                ExpectedResult(
                    description = "optionalNested.nestedStringA: should fail when longer than 2",
                    requestBody = expectedRequest(
                        optionalNested = expectedOptionalNested(
                            nestedStringA = "a".repeat(3)
                        )
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "optionalNested.nestedStringA",
                        error = "SizeConstraintViolated",
                        message = "must be between 0 and 2",
                        params = mapOf("min" to "0", "max" to "2")
                    )
                ),
                ExpectedResult(
                    description = "optionalNested.nestedStringA: should pass when exactly 2 characters",
                    requestBody = expectedRequest(
                        optionalNested = expectedOptionalNested(
                            nestedStringA = "a".repeat(2)
                        )
                    ),
                    expectedValidationErrorResponse = null
                ),
                ExpectedResult(
                    description = "mandatoryNested: should fail when set to null",
                    requestBody = expectedRequest(
                        mandatoryNested = JsonNull
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "mandatoryNested",
                        error = "MustNotBeNull",
                        message = "must not be null"
                    )
                ),
                ExpectedResult(
                    description = "mandatoryNested: should fail when not provided",
                    requestBody = expectedRequest(
                        mandatoryNested = null
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "mandatoryNested",
                        error = "MustNotBeNull",
                        message = "must not be null"
                    )
                ),
                ExpectedResult(
                    description = "mandatoryNested.nestedStringB: should fail when longer than 5",
                    requestBody = expectedRequest(
                        mandatoryNested = expectedMandatoryNested(
                            nestedStringB = "a".repeat(6)
                        )
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "mandatoryNested.nestedStringB",
                        error = "SizeConstraintViolated",
                        message = "must be between 0 and 5",
                        params = mapOf("min" to "0", "max" to "5")
                    )
                ),
                ExpectedResult(
                    description = "mandatoryNested.nestedStringB: should pass when exactly 5 characters",
                    requestBody = expectedRequest(
                        mandatoryNested = expectedMandatoryNested(
                            nestedStringB = "a".repeat(5)
                        )
                    ),
                    expectedValidationErrorResponse = null
                ),
                ExpectedResult(
                    description = "mandatoryNested.deeplyNested.deeplyNestedStringA: should fail when longer than 3",
                    requestBody = expectedRequest(
                        mandatoryNested = expectedMandatoryNested(
                            deeplyNested = expectedDeeplyNested(
                                deeplyNestedStringA = "a".repeat(4)
                            )
                        )
                    ),
                    expectedValidationErrorResponse = errorResponse(
                        field = "mandatoryNested.deeplyNested.deeplyNestedStringA",
                        error = "SizeConstraintViolated",
                        message = "must be between 0 and 3",
                        params = mapOf("min" to "0", "max" to "3")
                    )
                ),
                ExpectedResult(
                    description = "mandatoryNested.deeplyNested.deeplyNestedStringA: should pass when exactly 3 characters",
                    requestBody = expectedRequest(
                        mandatoryNested = expectedMandatoryNested(
                            deeplyNested = expectedDeeplyNested(
                                deeplyNestedStringA = "a".repeat(3)
                            )
                        )
                    ),
                    expectedValidationErrorResponse = null
                ),
            )
    }

    private fun expectedRequest(
        mandatoryStringA: JsonElement? = JsonPrimitive("x".repeat(5)),
        optionalStringA: String = "x".repeat(17),
        optionalStringMinMaxEqual: String = "x".repeat(13),
        optionalNested: JsonElement = expectedOptionalNested(),
        mandatoryNested: JsonElement? = expectedMandatoryNested(),
    ): JsonElement = buildJsonObject {
        if (mandatoryStringA != null) put("mandatoryStringA", mandatoryStringA)
        put("optionalStringA", optionalStringA)
        put("optionalStringMinMaxEqual", optionalStringMinMaxEqual)
        put("emptyNested", buildJsonObject { })
        put("optionalNested", optionalNested)
        if (mandatoryNested != null) put("mandatoryNested", mandatoryNested)
    }

    private fun expectedOptionalNested(
        nestedStringA: String = "x".repeat(1),
    ): JsonElement = buildJsonObject {
        put("nestedStringA", nestedStringA)
    }

    private fun expectedDeeplyNested(
        deeplyNestedStringA: String = "x".repeat(1),
    ): JsonElement = buildJsonObject {
        put("deeplyNestedStringA", deeplyNestedStringA)
    }

    private fun expectedMandatoryNested(
        nestedStringB: String = "x".repeat(2),
        deeplyNested: JsonElement = expectedDeeplyNested(),
    ): JsonElement = buildJsonObject {
        put("nestedStringB", nestedStringB)
        put("deeplyNested", deeplyNested)
    }

    private fun errorResponse(field: String, error: String, message: String, params: Map<String, String>? = null) =
        buildJsonObject {
            put("error", "InvalidInput")
            putJsonArray("requestErrors") {
                add(buildJsonObject {
                    put("field", field)
                    put("error", error)
                    put("message", message)
                    params?.let { params ->
                        put("params", buildJsonObject {
                            params.forEach { (key, value) ->
                                put(key, value)
                            }
                        })
                    }
                })
            }
        }

    private data class ExpectedResult(
        val description: String,
        val requestBody: JsonElement,
        val expectedValidationErrorResponse: JsonElement?
    )

    private fun Arguments.toExpectedResult(): ExpectedResult {
        val testCase = this.get()
            .shouldHaveSize(1)[0]
            .shouldBeInstanceOf<ApiRequestsValidationsTestCase>()

        return ExpectedResult(
            description = testCase.description,
            requestBody = testCase.requestBody!!,
            expectedValidationErrorResponse = testCase.expectedValidationErrorResponse,
        )
    }
}
