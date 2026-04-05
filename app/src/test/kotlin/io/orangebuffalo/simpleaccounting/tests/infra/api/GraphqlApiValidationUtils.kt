package io.orangebuffalo.simpleaccounting.tests.infra.api

import io.orangebuffalo.simpleaccounting.infra.graphql.DgsClient
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection

/**
 * Base interface for parameterized GraphQL mutation input validation test cases.
 *
 * Implementations represent different categories of input validation:
 * - [GraphqlMutationValidationErrorTestCase] — invalid input that should produce a specific `FIELD_VALIDATION_FAILURE` error
 * - [GraphqlMutationRejectedInputTestCase] — null input for a non-null field, rejected by GraphQL type system
 * - [GraphqlMutationValidBoundaryTestCase] — valid boundary value that should pass validation and execute successfully
 *
 * Use [mustNotBeBlankTestCases] and [sizeConstraintTestCases] to generate test cases declaratively.
 * Execute with [GraphqlClientRequestExecutor.executeAndVerifyInputValidation].
 */
sealed interface GraphqlMutationInputTestCase {
    val description: String
}

/**
 * Test case for invalid input that should produce a `FIELD_VALIDATION_FAILURE` error.
 * The assertion verifies the exact error structure including violation path, error code, message, and optional params.
 *
 * @property mutation the DGS mutation projection that produces the invalid input
 * @property violationPath the expected field path in the validation error (e.g. `"currentPassword"`)
 * @property error the expected validation error code (e.g. `"MustNotBeBlank"`, `"SizeConstraintViolated"`)
 * @property message the expected validation error message
 * @property params optional validation error parameters (e.g. `min`/`max` for size constraints)
 */
data class GraphqlMutationValidationErrorTestCase(
    override val description: String,
    val mutation: MutationProjection.() -> MutationProjection,
    val violationPath: String,
    val error: String,
    val message: String,
    val params: Map<String, String>? = null,
) : GraphqlMutationInputTestCase {
    override fun toString() = description
}

/**
 * Test case for null input sent to a non-null GraphQL field.
 * The null value is embedded directly in the raw GraphQL query (not via variables),
 * bypassing DGS type-safe builder's null checks.
 *
 * The assertion verifies that GraphQL returns a `ValidationError` mentioning the [fieldName].
 *
 * @property rawQueryBuilder lazily builds the raw GraphQL query with null for the target field.
 *   Lazy to avoid accessing test preconditions during test discovery (JUnit `@MethodSource`).
 * @property fieldName the GraphQL field name that receives null, used for error message assertion
 */
data class GraphqlMutationRejectedInputTestCase(
    override val description: String,
    val rawQueryBuilder: () -> String,
    val fieldName: String,
) : GraphqlMutationInputTestCase {
    override fun toString() = description
}

/**
 * Test case for a valid boundary value that should pass input validation and
 * result in a fully successful mutation execution (no errors in the response).
 *
 * @property mutation the DGS mutation projection with the boundary value
 * @property setup optional callback executed before the request, typically used to set up mocks
 *   that allow the mutation's business logic to succeed with the boundary value
 */
data class GraphqlMutationValidBoundaryTestCase(
    override val description: String,
    val mutation: MutationProjection.() -> MutationProjection,
    val setup: () -> Unit = {},
) : GraphqlMutationInputTestCase {
    override fun toString() = description
}

/**
 * Generates test cases for `@NotBlank` string field validation. Produces:
 * - **null** input → rejected by GraphQL type system (`ValidationError`)
 * - **blank** input (`"  "`) → `FIELD_VALIDATION_FAILURE` with `MustNotBeBlank`
 * - **empty** input (`""`) → `FIELD_VALIDATION_FAILURE` with `MustNotBeBlank`
 * - **min valid length** boundary → fully successful execution
 *
 * @param fieldName the GraphQL field name being validated
 * @param minValidLength the minimum string length that passes validation (default 1).
 *   Override when the field has a `@Size(min = N)` constraint with N > 1.
 * @param boundarySetup optional setup executed before boundary tests to ensure
 *   the mutation succeeds with valid boundary values (e.g. mock authentication)
 * @param mutationWithFieldValue builds the mutation with the test field set to the given value,
 *   while other fields use valid defaults
 */
fun mustNotBeBlankTestCases(
    fieldName: String,
    minValidLength: Int = 1,
    boundarySetup: () -> Unit = {},
    mutationWithFieldValue: MutationProjection.(fieldValue: String) -> MutationProjection,
): List<GraphqlMutationInputTestCase> {
    return listOf(
        GraphqlMutationRejectedInputTestCase(
            description = "$fieldName is null",
            fieldName = fieldName,
            rawQueryBuilder = {
                buildRawMutationQueryWithNullField(fieldName) { mutationWithFieldValue("validPlaceholder") }
            },
        ),
        GraphqlMutationValidationErrorTestCase(
            description = "$fieldName is blank",
            mutation = { mutationWithFieldValue("  ") },
            violationPath = fieldName,
            error = "MustNotBeBlank",
            message = "must not be blank",
        ),
        GraphqlMutationValidationErrorTestCase(
            description = "$fieldName is empty",
            mutation = { mutationWithFieldValue("") },
            violationPath = fieldName,
            error = "MustNotBeBlank",
            message = "must not be blank",
        ),
        GraphqlMutationValidBoundaryTestCase(
            description = "$fieldName with min valid length ($minValidLength) is accepted",
            mutation = { mutationWithFieldValue("a".repeat(minValidLength)) },
            setup = boundarySetup,
        ),
    )
}

/**
 * Generates test cases for `@Size` string field validation. Produces:
 * - **exceeds max length** → `FIELD_VALIDATION_FAILURE` with `SizeConstraintViolated`
 * - **below min length** (if `minLength > 1`) → `FIELD_VALIDATION_FAILURE` with `SizeConstraintViolated`
 * - **at max length** boundary → fully successful execution
 * - **at min length** boundary (if `minLength > 1` and differs from `maxLength`) → fully successful execution
 *
 * @param fieldName the GraphQL field name being validated
 * @param maxLength the maximum allowed length from `@Size(max = ...)`
 * @param minLength the minimum allowed length from `@Size(min = ...)`, defaults to 0
 * @param boundarySetup optional setup executed before boundary tests
 * @param mutationWithFieldValue builds the mutation with the test field set to the given value
 */
fun sizeConstraintTestCases(
    fieldName: String,
    maxLength: Int,
    minLength: Int = 0,
    boundarySetup: () -> Unit = {},
    mutationWithFieldValue: MutationProjection.(fieldValue: String) -> MutationProjection,
): List<GraphqlMutationInputTestCase> = listOfNotNull(
    GraphqlMutationValidationErrorTestCase(
        description = "$fieldName exceeds max length ($maxLength)",
        mutation = { mutationWithFieldValue("a".repeat(maxLength + 1)) },
        violationPath = fieldName,
        error = "SizeConstraintViolated",
        message = "size must be between $minLength and $maxLength",
        params = mapOf("min" to "$minLength", "max" to "$maxLength"),
    ),
    if (minLength > 1) GraphqlMutationValidationErrorTestCase(
        description = "$fieldName below min length ($minLength)",
        mutation = { mutationWithFieldValue("a".repeat(minLength - 1)) },
        violationPath = fieldName,
        error = "SizeConstraintViolated",
        message = "size must be between $minLength and $maxLength",
        params = mapOf("min" to "$minLength", "max" to "$maxLength"),
    ) else null,
    GraphqlMutationValidBoundaryTestCase(
        description = "$fieldName at max length ($maxLength) is accepted",
        mutation = { mutationWithFieldValue("a".repeat(maxLength)) },
        setup = boundarySetup,
    ),
    if (minLength > 1 && minLength != maxLength) GraphqlMutationValidBoundaryTestCase(
        description = "$fieldName at min length ($minLength) is accepted",
        mutation = { mutationWithFieldValue("a".repeat(minLength)) },
        setup = boundarySetup,
    ) else null,
)

/**
 * Generates test cases for `@Min` and `@Max` integer field validation. Produces:
 * - **below minimum** → `FIELD_VALIDATION_FAILURE` with `MinConstraintViolated`
 * - **above maximum** → `FIELD_VALIDATION_FAILURE` with `MaxConstraintViolated`
 * - **at minimum** boundary → fully successful execution
 * - **at maximum** boundary → fully successful execution
 *
 * @param fieldName the GraphQL field name being validated
 * @param minValue the minimum allowed value from `@Min(value = ...)`
 * @param maxValue the maximum allowed value from `@Max(value = ...)`
 * @param boundarySetup optional setup executed before boundary tests
 * @param mutationWithFieldValue builds the mutation with the test field set to the given integer value
 */
fun numberRangeConstraintTestCases(
    fieldName: String,
    minValue: Int,
    maxValue: Int,
    boundarySetup: () -> Unit = {},
    mutationWithFieldValue: MutationProjection.(fieldValue: Int) -> MutationProjection,
): List<GraphqlMutationInputTestCase> = listOf(
    GraphqlMutationValidationErrorTestCase(
        description = "$fieldName below minimum ($minValue)",
        mutation = { mutationWithFieldValue(minValue - 1) },
        violationPath = fieldName,
        error = "MinConstraintViolated",
        message = "must be greater than or equal to $minValue",
        params = mapOf("value" to "$minValue"),
    ),
    GraphqlMutationValidationErrorTestCase(
        description = "$fieldName above maximum ($maxValue)",
        mutation = { mutationWithFieldValue(maxValue + 1) },
        violationPath = fieldName,
        error = "MaxConstraintViolated",
        message = "must be less than or equal to $maxValue",
        params = mapOf("value" to "$maxValue"),
    ),
    GraphqlMutationValidBoundaryTestCase(
        description = "$fieldName at minimum ($minValue) is accepted",
        mutation = { mutationWithFieldValue(minValue) },
        setup = boundarySetup,
    ),
    GraphqlMutationValidBoundaryTestCase(
        description = "$fieldName at maximum ($maxValue) is accepted",
        mutation = { mutationWithFieldValue(maxValue) },
        setup = boundarySetup,
    ),
)

/**
 * Builds a raw GraphQL mutation query with the specified field set to `null`.
 * Works with any scalar type (String, Int, Boolean, enum) by replacing the field's
 * serialized value in the generated query text.
 *
 * @param fieldName the field name whose value should be replaced with null
 * @param mutationBuilder builds the mutation with a non-null placeholder value for the target field
 */
private fun buildRawMutationQueryWithNullField(
    fieldName: String,
    mutationBuilder: MutationProjection.() -> MutationProjection,
): String {
    val query = DgsClient.buildMutation(_projection = mutationBuilder)
        .lines()
        // DGS adds __typename to every projection; removed here since raw queries
        // are only used for null-input testing and __typename is not relevant
        .filter { it.trim() != "__typename" }
        .joinToString("\n")
    return query.replace(
        // Matches fieldName followed by any scalar value: strings ("..."), numbers, booleans, or enum identifiers
        Regex("""(\b${Regex.escape(fieldName)}\s*:\s*)(?:"(?:[^"\\]|\\.)*"|-?\d+(?:\.\d+)?|true|false|\w+)"""),
        "$1null"
    )
}
