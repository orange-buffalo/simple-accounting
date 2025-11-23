package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import graphql.introspection.Introspection

const val MAX_LENGTH_DIRECTIVE_NAME = "maxLength"

@GraphQLDirective(
    name = MAX_LENGTH_DIRECTIVE_NAME,
    description = "Validates that the string value length does not exceed the specified maximum. " +
            "If validation fails, returns an error with `FIELD_VALIDATION_FAILURE` error type.",
    locations = [
        Introspection.DirectiveLocation.ARGUMENT_DEFINITION,
    ]
)
annotation class MaxLength(
    @param:GraphQLDescription("The maximum allowed length of the string value.")
    val value: Int
)
