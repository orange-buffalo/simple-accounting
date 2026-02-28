package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import graphql.introspection.Introspection

@GraphQLDirective(
    name = "slowOperation",
    description = "Marks an operation as potentially slow, as it may interact with third-party systems. " +
            "Clients are recommended to invoke such operations separately from other queries, " +
            "with longer timeouts and appropriate long-running operation handling (e.g. loading indicators).",
    locations = [
        Introspection.DirectiveLocation.FIELD_DEFINITION,
    ]
)
annotation class SlowOperation
