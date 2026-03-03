package io.orangebuffalo.simpleaccounting.tests.infra.api

import io.orangebuffalo.simpleaccounting.infra.graphql.DgsClient
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection

private const val NULL_PLACEHOLDER = "\u0000SA_NULL\u0000"

sealed interface GraphqlMutationInputTestCase {
    val description: String
}

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

data class GraphqlMutationRejectedInputTestCase(
    override val description: String,
    val rawQueryBuilder: () -> String,
) : GraphqlMutationInputTestCase {
    override fun toString() = description
}

data class GraphqlMutationValidBoundaryTestCase(
    override val description: String,
    val mutation: MutationProjection.() -> MutationProjection,
) : GraphqlMutationInputTestCase {
    override fun toString() = description
}

fun mustNotBeBlankTestCases(
    fieldName: String,
    minValidLength: Int = 1,
    mutationWithFieldValue: (fieldValue: String) -> MutationProjection.() -> MutationProjection,
): List<GraphqlMutationInputTestCase> {
    return listOf(
        GraphqlMutationRejectedInputTestCase(
            description = "$fieldName is null",
            rawQueryBuilder = { buildRawQueryWithNull(mutationWithFieldValue) },
        ),
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
            description = "$fieldName with min valid length ($minValidLength) is accepted",
            mutation = mutationWithFieldValue("a".repeat(minValidLength)),
        ),
    )
}

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
    if (minLength > 1 && minLength != maxLength) GraphqlMutationValidBoundaryTestCase(
        description = "$fieldName at min length ($minLength) is accepted",
        mutation = mutationWithFieldValue("a".repeat(minLength)),
    ) else null,
)

private fun buildRawQueryWithNull(
    mutationWithFieldValue: (fieldValue: String) -> MutationProjection.() -> MutationProjection,
): String = DgsClient.buildMutation(_projection = mutationWithFieldValue(NULL_PLACEHOLDER))
    .lines()
    .filter { it.trim() != "__typename" }
    .joinToString("\n")
    .replace("\"$NULL_PLACEHOLDER\"", "null")
