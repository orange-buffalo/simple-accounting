package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import graphql.introspection.Introspection

const val VALIDATED_DIRECTIVE_NAME = "validated"

@GraphQLDirective(
    name = VALIDATED_DIRECTIVE_NAME,
    description = "Marks a field as requiring validation. " +
            "All arguments with @notBlank or @maxLength directives will be validated. " +
            "If validation fails, returns an error with `FIELD_VALIDATION_FAILURE` error type.",
    locations = [
        Introspection.DirectiveLocation.FIELD_DEFINITION,
    ]
)
annotation class Validated
