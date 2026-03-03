package io.orangebuffalo.simpleaccounting.tests.infra.api

import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection

sealed interface GraphqlMutationInputTestCase {
    val description: String
    val mutation: MutationProjection.() -> MutationProjection
}

data class GraphqlMutationValidationErrorTestCase(
    override val description: String,
    override val mutation: MutationProjection.() -> MutationProjection,
    val violationPath: String,
    val error: String,
    val message: String,
    val params: Map<String, String>? = null,
) : GraphqlMutationInputTestCase {
    override fun toString() = description
}

data class GraphqlMutationValidBoundaryTestCase(
    override val description: String,
    override val mutation: MutationProjection.() -> MutationProjection,
) : GraphqlMutationInputTestCase {
    override fun toString() = description
}

fun mustNotBeBlankTestCases(
    fieldName: String,
    mutationWithFieldValue: (fieldValue: String) -> MutationProjection.() -> MutationProjection,
): List<GraphqlMutationInputTestCase> = listOf(
    GraphqlMutationValidationErrorTestCase(
        description = "$fieldName is blank",
        mutation = mutationWithFieldValue("  "),
        violationPath = fieldName,
        error = "MustNotBeBlank",
        message = "must not be blank",
    ),
    GraphqlMutationValidationErrorTestCase(
        description = "$fieldName is empty",
        mutation = mutationWithFieldValue(""),
        violationPath = fieldName,
        error = "MustNotBeBlank",
        message = "must not be blank",
    ),
    GraphqlMutationValidBoundaryTestCase(
        description = "$fieldName with single character is accepted",
        mutation = mutationWithFieldValue("a"),
    ),
)

fun sizeConstraintTestCases(
    fieldName: String,
    maxLength: Int,
    minLength: Int = 0,
    mutationWithFieldValue: (fieldValue: String) -> MutationProjection.() -> MutationProjection,
): List<GraphqlMutationInputTestCase> = listOfNotNull(
    GraphqlMutationValidationErrorTestCase(
        description = "$fieldName exceeds max length ($maxLength)",
        mutation = mutationWithFieldValue("a".repeat(maxLength + 1)),
        violationPath = fieldName,
        error = "SizeConstraintViolated",
        message = "size must be between $minLength and $maxLength",
        params = mapOf("min" to "$minLength", "max" to "$maxLength"),
    ),
    if (minLength > 1) GraphqlMutationValidationErrorTestCase(
        description = "$fieldName below min length ($minLength)",
        mutation = mutationWithFieldValue("a".repeat(minLength - 1)),
        violationPath = fieldName,
        error = "SizeConstraintViolated",
        message = "size must be between $minLength and $maxLength",
        params = mapOf("min" to "$minLength", "max" to "$maxLength"),
    ) else null,
    GraphqlMutationValidBoundaryTestCase(
        description = "$fieldName at max length ($maxLength) is accepted",
        mutation = mutationWithFieldValue("a".repeat(maxLength)),
    ),
    // no separate min-length boundary when minLength == maxLength, as max-length test already covers it
    if (minLength > 1 && minLength != maxLength) GraphqlMutationValidBoundaryTestCase(
        description = "$fieldName at min length ($minLength) is accepted",
        mutation = mutationWithFieldValue("a".repeat(minLength)),
    ) else null,
)
