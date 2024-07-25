package io.orangebuffalo.simpleaccounting.tests.infra.utils

import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyBadRequestAndJsonBody
import kotlinx.serialization.json.*
import mu.KotlinLogging
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.stream.Stream

typealias ApiRequestsValidationsTestRequestExecutionSpec = (
    requestBody: String
) -> WebTestClient.RequestHeadersSpec<*>

typealias ApiRequestsBodyConfiguration = ApiRequestsValidationsBodySpec.() -> Unit

/**
 * Base class for testing API requests validations.
 * Typically, a nested class in the API operation nested class will extend this base class.
 * The implementation needs to provide a request execution logic, that accepts the JSON request
 * body. The various body configurations are created based on the request body spec and cover
 * all standard validation scenarios (e.g. missing and null values for mandatory fields,
 * min and max length for strings, etc.).
 */
@DisplayName("Should validate requests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ApiRequestsValidationsTestBase {
    protected abstract val requestExecutionSpec: ApiRequestsValidationsTestRequestExecutionSpec
    protected abstract val requestBodySpec: ApiRequestsBodyConfiguration
    protected open val successResponseStatus: HttpStatus = HttpStatus.OK

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(ApiRequestsValidationsArgumentsProvider::class)
    fun `should validate requests`(testCase: ApiRequestsValidationsTestCase) {
        log.debug { "Executing request:\n${testCase.requestBody}" }

        if (testCase.expectedValidationErrorResponse == null) {
            requestExecutionSpec(testCase.requestBody.toString())
                .exchange()
                .expectStatus().isEqualTo(successResponseStatus)
        } else {
            requestExecutionSpec(testCase.requestBody.toString())
                .verifyBadRequestAndJsonBody(testCase.expectedValidationErrorResponse.toString())
        }
    }

    /**
     * Generates arguments for [ApiRequestsValidationsTestBase] based on request spec.
     */
    class ApiRequestsValidationsArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments> {
            val testInstance = extensionContext.testInstance.get() as ApiRequestsValidationsTestBase
            val requestBodyRoot = ApiRequestsValidationsObjectFieldSpec(name = "", mandatory = false)
            testInstance.requestBodySpec(requestBodyRoot)
            val allTestCases = mutableListOf<ApiRequestsValidationsTestCase>()

            allTestCases.add(
                ApiRequestsValidationsTestCase(
                    description = "Sanity check: all values at their default should succeed",
                    requestBody = requestBodyRoot.defaultValidJsonRequestValue(),
                    expectedValidationErrorResponse = null
                )
            )

            allTestCases.addAll(requestBodyRoot.invalidCases().filterNotNull().map { errorCase ->
                ApiRequestsValidationsTestCase(
                    description = "${errorCase.fullPath}: ${errorCase.description}",
                    requestBody = errorCase.requestFieldValue,
                    expectedValidationErrorResponse = errorCase.expectedResponse
                )
            })

            allTestCases.addAll(requestBodyRoot.validBoundaryCases().filterNotNull().map { boundaryCase ->
                ApiRequestsValidationsTestCase(
                    description = "${boundaryCase.fullPath}: ${boundaryCase.description}",
                    requestBody = boundaryCase.requestFieldValue,
                    expectedValidationErrorResponse = null
                )
            })

            return allTestCases.stream()
                .map { Arguments.of(it) }
        }
    }
}

/**
 * API for configuring the request body validation scenarios.
 * The implementation should define each field in the request schema, including
 * nested objects and arrays. Each field should provide its validation requirements
 * to generate scenarios for.
 */
interface ApiRequestsValidationsBodySpec {
    /**
     * Defines a string field in the request body.
     */
    fun string(
        name: String,
        mandatory: Boolean,
        minLength: Int = 0,
        maxLength: Int,
        defaultValidValue: String = "x".repeat((maxLength + minLength) / 2)
    )

    /**
     * Defines a boolean field in the request body.
     */
    fun boolean(
        name: String,
        mandatory: Boolean,
        defaultValidValue: Boolean = true
    )

    /**
     * Defines a nested object field in the request body.
     */
    fun nested(
        name: String,
        mandatory: Boolean,
        spec: ApiRequestsBodyConfiguration
    )
}

data class ApiRequestsValidationsTestCase(
    val description: String,
    val requestBody: JsonElement?,
    val expectedValidationErrorResponse: JsonElement?
) {
    override fun toString() = description
}

private sealed class ApiRequestsValidationsFieldSpec<T : Any>(
    val name: String,
    val mandatory: Boolean
) {
    abstract fun invalidCases(): List<ApiRequestsValidationsErrorCase?>
    abstract fun defaultValidJsonRequestValue(): JsonElement
    abstract fun validBoundaryCases(): List<ApiRequestsValidationsValidBoundaryCase?>
}

private class ApiRequestsValidationsStringFieldSpec(
    name: String,
    mandatory: Boolean,
    private val minLength: Int,
    private val maxLength: Int,
    private val defaultValidValue: String
) : ApiRequestsValidationsFieldSpec<String>(
    name = name,
    mandatory = mandatory,
) {
    init {
        require(minLength >= 0) { "$name: minLength $minLength should be >= 0" }
        require(maxLength >= 0) { "$name: maxLength $maxLength should be >= 0" }
        require(maxLength >= minLength) { "$name: maxLength $maxLength should be >= minLength $minLength" }
        require(defaultValidValue.length in minLength..maxLength) {
            "$name: defaultValidValue $defaultValidValue should be between minLength and maxLength ($minLength, $maxLength)"
        }
    }

    override fun invalidCases() = listOf(
        if (mandatory) ApiRequestsValidationsErrorCase(
            field = name,
            description = "should fail when null",
            requestFieldValue = JsonNull,
            expectedValidationError = ExpectedErrorData.notNull(),
        ) else null,
        if (mandatory) ApiRequestsValidationsErrorCase(
            field = name,
            description = "should fail when not provided",
            requestFieldValue = null,
            expectedValidationError = ExpectedErrorData.notNull(),
        ) else null,
        if (mandatory) ApiRequestsValidationsErrorCase(
            field = name,
            description = "should fail when empty string",
            requestFieldValue = JsonPrimitive(""),
            expectedValidationError = ExpectedErrorData.notBlank(),
        ) else null,
        if (mandatory) ApiRequestsValidationsErrorCase(
            field = name,
            description = "should fail when blank string",
            requestFieldValue = JsonPrimitive(" ".repeat(minLength + 1)),
            expectedValidationError = ExpectedErrorData.notBlank(),
        ) else null,
        ApiRequestsValidationsErrorCase(
            field = name,
            description = "should fail when longer than $maxLength",
            requestFieldValue = JsonPrimitive("a".repeat(maxLength + 1)),
            expectedValidationError = ExpectedErrorData.size(
                minLength = minLength,
                maxLength = maxLength
            )
        ),
        if (minLength > 1) ApiRequestsValidationsErrorCase(
            field = name,
            description = "should fail when shorter than $minLength",
            requestFieldValue = JsonPrimitive("a".repeat(minLength - 1)),
            expectedValidationError = ExpectedErrorData.size(
                minLength = minLength,
                maxLength = maxLength
            )
        ) else null,
    )

    override fun defaultValidJsonRequestValue() = JsonPrimitive(defaultValidValue)

    override fun validBoundaryCases() = listOf(
        ApiRequestsValidationsValidBoundaryCase(
            field = name,
            description = "should pass when exactly $maxLength characters",
            requestFieldValue = JsonPrimitive("a".repeat(maxLength))
        ),
        if (minLength > 1 && minLength != maxLength) ApiRequestsValidationsValidBoundaryCase(
            field = name,
            description = "should pass when exactly $minLength characters",
            requestFieldValue = JsonPrimitive("a".repeat(minLength))
        ) else null
    )
}

private class ApiRequestsValidationsBooleanFieldSpec(
    name: String,
    mandatory: Boolean,
    private val defaultValidValue: Boolean
) : ApiRequestsValidationsFieldSpec<Boolean>(
    name = name,
    mandatory = mandatory,
) {
    override fun invalidCases() = listOf(
        if (mandatory) ApiRequestsValidationsErrorCase(
            field = name,
            description = "should fail when null",
            requestFieldValue = JsonNull,
            expectedValidationError = ExpectedErrorData.notNull(),
        ) else null,
        if (mandatory) ApiRequestsValidationsErrorCase(
            field = name,
            description = "should fail when not provided",
            requestFieldValue = null,
            expectedValidationError = ExpectedErrorData.notNull(),
        ) else null,
    )

    override fun defaultValidJsonRequestValue() = JsonPrimitive(defaultValidValue)

    override fun validBoundaryCases() = emptyList<ApiRequestsValidationsValidBoundaryCase>()
}

private class ApiRequestsValidationsObjectFieldSpec(
    name: String,
    mandatory: Boolean
) : ApiRequestsValidationsFieldSpec<Any>(name, mandatory), ApiRequestsValidationsBodySpec {
    private val fields: MutableList<ApiRequestsValidationsFieldSpec<*>> = mutableListOf()

    override fun string(
        name: String,
        mandatory: Boolean,
        minLength: Int,
        maxLength: Int,
        defaultValidValue: String
    ) {
        fields.add(
            ApiRequestsValidationsStringFieldSpec(
                name = name,
                mandatory = mandatory,
                minLength = minLength,
                maxLength = maxLength,
                defaultValidValue = defaultValidValue
            )
        )
    }

    override fun boolean(name: String, mandatory: Boolean, defaultValidValue: Boolean) {
        fields.add(
            ApiRequestsValidationsBooleanFieldSpec(
                name = name,
                mandatory = mandatory,
                defaultValidValue = defaultValidValue
            )
        )
    }

    override fun nested(name: String, mandatory: Boolean, spec: ApiRequestsBodyConfiguration) {
        val nestedField = ApiRequestsValidationsObjectFieldSpec(name, mandatory)
        spec(nestedField)
        fields.add(nestedField)
    }

    override fun invalidCases(): List<ApiRequestsValidationsErrorCase?> {
        val allCases = mutableListOf<ApiRequestsValidationsErrorCase>()

        if (mandatory) {
            allCases.add(
                ApiRequestsValidationsErrorCase(
                    field = name,
                    description = "should fail when set to null",
                    requestFieldValue = JsonNull,
                    expectedValidationError = ExpectedErrorData.notNull()
                )
            )
            allCases.add(
                ApiRequestsValidationsErrorCase(
                    field = name,
                    description = "should fail when not provided",
                    requestFieldValue = null,
                    expectedValidationError = ExpectedErrorData.notNull()
                )
            )
        }

        fields.forEach { field ->
            field.invalidCases().filterNotNull().forEach { fieldErrorCase ->
                val thisJsonValue = buildJsonObject {
                    fields.filter { it != field }.forEach { validField ->
                        put(validField.name, validField.defaultValidJsonRequestValue())
                    }
                    if (fieldErrorCase.requestFieldValue != null) {
                        put(field.name, fieldErrorCase.requestFieldValue)
                    }
                }
                allCases.add(
                    ApiRequestsValidationsErrorCase(
                        field = name,
                        nestedPath = field.name + fieldErrorCase.nestedPath.asPathLastPart,
                        description = fieldErrorCase.description,
                        requestFieldValue = thisJsonValue,
                        expectedValidationError = fieldErrorCase.expectedValidationError
                    )
                )
            }
        }

        return allCases
    }

    override fun defaultValidJsonRequestValue(): JsonElement {
        return buildJsonObject {
            fields.forEach { field ->
                put(field.name, field.defaultValidJsonRequestValue())
            }
        }
    }

    override fun validBoundaryCases(): List<ApiRequestsValidationsValidBoundaryCase?> {
        val allCases = mutableListOf<ApiRequestsValidationsValidBoundaryCase>()

        fields.forEach { field ->
            field.validBoundaryCases().filterNotNull().forEach { fieldBoundaryCase ->
                val thisJsonValue = buildJsonObject {
                    fields.filter { it != field }.forEach { validField ->
                        put(validField.name, validField.defaultValidJsonRequestValue())
                    }
                    put(field.name, fieldBoundaryCase.requestFieldValue)
                }
                allCases.add(
                    ApiRequestsValidationsValidBoundaryCase(
                        field = name,
                        nestedPath = field.name + fieldBoundaryCase.nestedPath.asPathLastPart,
                        description = fieldBoundaryCase.description,
                        requestFieldValue = thisJsonValue
                    )
                )
            }
        }

        return allCases
    }
}

private data class ApiRequestsValidationsValidBoundaryCase(
    val field: String,
    val nestedPath: String = "",
    val description: String,
    val requestFieldValue: JsonElement,
) {
    val fullPath: String = getFullPath(field, nestedPath)
}

private val String.asPathLastPart: String
    get() = if (this.isEmpty()) "" else ".$this"

private data class ApiRequestsValidationsErrorCase(
    val field: String,
    val nestedPath: String = "",
    val description: String,
    val requestFieldValue: JsonElement?,
    val expectedValidationError: ExpectedErrorData
) {
    val fullPath: String = getFullPath(field, nestedPath)

    val expectedResponse: JsonElement = buildJsonObject {
        put("error", JsonPrimitive("InvalidInput"))
        put("requestErrors", buildJsonArray {
            add(
                buildJsonObject {
                    put(
                        "field",
                        JsonPrimitive(fullPath)
                    )
                    put("error", JsonPrimitive(expectedValidationError.error))
                    put("message", JsonPrimitive(expectedValidationError.message))
                    if (expectedValidationError.params.isNotEmpty()) {
                        put("params", buildJsonObject {
                            expectedValidationError.params.forEach { (key, value) ->
                                put(key, JsonPrimitive(value))
                            }
                        })
                    }
                }
            )
        })
    }
}

private data class ExpectedErrorData(
    val error: String,
    val message: String,
    val params: Map<String, String> = mapOf(),
) {
    companion object {

        fun notNull() = ExpectedErrorData(
            error = "MustNotBeNull",
            message = "must not be null",
        )

        fun notBlank() = ExpectedErrorData(
            error = "MustNotBeBlank",
            message = "must not be blank",
        )

        fun size(minLength: Int = 0, maxLength: Int) = ExpectedErrorData(
            error = "SizeConstraintViolated",
            message = "size must be between $minLength and $maxLength",
            params = mapOf(
                "min" to minLength.toString(),
                "max" to maxLength.toString()
            )
        )
    }
}

private fun getFullPath(
    field: String,
    nestedPath: String,
) = field + (if (field.isNotEmpty() && nestedPath.isNotEmpty()) "." else "") + nestedPath

private val log = KotlinLogging.logger { }
