package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import graphql.introspection.Introspection

const val NOT_BLANK_DIRECTIVE_NAME = "notBlank"

@GraphQLDirective(
    name = NOT_BLANK_DIRECTIVE_NAME,
    description = "Validates that the string value is not null, not empty, and contains at least one non-whitespace character. " +
            "If validation fails, returns an error with `FIELD_VALIDATION_FAILURE` error type.",
    locations = [
        Introspection.DirectiveLocation.ARGUMENT_DEFINITION,
    ]
)
annotation class NotBlank
