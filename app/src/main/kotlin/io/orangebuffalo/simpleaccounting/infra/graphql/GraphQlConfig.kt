package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.federation.directives.ContactDirective
import com.expediagroup.graphql.server.Schema
import org.springframework.stereotype.Component

@ContactDirective(
    name = "Simple Accounting",
    url = "https://github.com/orange-buffalo/simple-accounting/issues",
    description = "For any questions, issues or feature requests, please open an issue or discussion on GitHub."
)
@GraphQLDescription("GraphQL schema for Simple Accounting application")
@Component
class SaGraphQlSchema : Schema
