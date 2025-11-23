package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import graphql.introspection.Introspection

const val NOT_BLANK_DIRECTIVE_NAME = "notBlank"

/**
 * Validates that annotated string arguments are not null, not empty, and contain at least one non-whitespace character.
 * Applied to arguments via Kotlin annotations, triggers validation at the field level.
 * If validation fails, returns an error with `FIELD_VALIDATION_FAILURE` error type.
 */
@GraphQLDirective(
    name = NOT_BLANK_DIRECTIVE_NAME,
    description = "Validates that string arguments are not null, not empty, and contain at least one non-whitespace character. " +
            "If validation fails, returns an error with `FIELD_VALIDATION_FAILURE` error type.",
    locations = [
        Introspection.DirectiveLocation.ARGUMENT_DEFINITION,
        Introspection.DirectiveLocation.FIELD_DEFINITION,
    ]
)
annotation class NotBlank
