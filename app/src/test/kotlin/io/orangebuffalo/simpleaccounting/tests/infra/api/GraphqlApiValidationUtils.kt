package io.orangebuffalo.simpleaccounting.tests.infra.api

import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection

data class GraphqlValidationTestCase(
    val description: String,
    val mutation: MutationProjection.() -> MutationProjection,
    val violationPath: String,
    val error: String,
    val message: String,
    val params: Map<String, String>? = null,
) {
    override fun toString() = description
}

fun mustNotBeBlankTestCases(
    fieldName: String,
    mutationWithFieldValue: (fieldValue: String) -> MutationProjection.() -> MutationProjection,
) = listOf(
    GraphqlValidationTestCase(
        description = "$fieldName is blank",
        mutation = mutationWithFieldValue("  "),
        violationPath = fieldName,
        error = "MustNotBeBlank",
        message = "must not be blank",
    ),
    GraphqlValidationTestCase(
        description = "$fieldName is empty",
        mutation = mutationWithFieldValue(""),
        violationPath = fieldName,
        error = "MustNotBeBlank",
        message = "must not be blank",
    ),
)

fun sizeConstraintTestCases(
    fieldName: String,
    maxLength: Int,
    minLength: Int = 0,
    mutationWithFieldValue: (fieldValue: String) -> MutationProjection.() -> MutationProjection,
) = listOf(
    GraphqlValidationTestCase(
        description = "$fieldName exceeds max length ($maxLength)",
        mutation = mutationWithFieldValue("a".repeat(maxLength + 1)),
        violationPath = fieldName,
        error = "SizeConstraintViolated",
        message = "size must be between $minLength and $maxLength",
        params = mapOf("min" to "$minLength", "max" to "$maxLength"),
    ),
)
