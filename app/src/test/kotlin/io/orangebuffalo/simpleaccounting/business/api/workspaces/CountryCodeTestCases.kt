package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.GraphqlMutationInputTestCase
import io.orangebuffalo.simpleaccounting.tests.infra.api.GraphqlMutationValidationErrorTestCase
import io.orangebuffalo.simpleaccounting.tests.infra.api.GraphqlMutationValidBoundaryTestCase
import io.orangebuffalo.simpleaccounting.tests.infra.api.mustNotBeBlankTestCases

fun countryCodeTestCases(
    mutationWithValue: MutationProjection.(String) -> MutationProjection,
): List<GraphqlMutationInputTestCase> = mustNotBeBlankTestCases(
    "residency", mutationWithFieldValue = mutationWithValue,
).filterNot { it is GraphqlMutationValidBoundaryTestCase } +
    listOf("ZZ", "au", "AUS", "AUD", "A").map { value ->
        GraphqlMutationValidationErrorTestCase(
            description = "residency rejects $value",
            mutation = { mutationWithValue(value) },
            violationPath = "residency",
            error = "MustBeValidCountryCode",
            message = "must be a valid ISO 3166-1 alpha-2 country code",
        )
    } + listOf("AU", "UA", "US").map { value ->
        GraphqlMutationValidBoundaryTestCase(
            description = "residency accepts $value",
            mutation = { mutationWithValue(value) },
        )
    }
