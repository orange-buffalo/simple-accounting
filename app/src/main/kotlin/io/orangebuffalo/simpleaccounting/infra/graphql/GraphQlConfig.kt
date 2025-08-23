package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import com.expediagroup.graphql.generator.federation.directives.ContactDirective
import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
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

@Component
class SaSchemaGeneratorHooks : SchemaGeneratorHooks {
    private val directiveWiringFactory = KotlinDirectiveWiringFactory(
        manualWiring = mapOf<String, KotlinSchemaDirectiveWiring>(
            REQUIRED_AUTH_DIRECTIVE_NAME to RequiredAuthDirectiveWiring(),
        )
    )
    override val wiringFactory: KotlinDirectiveWiringFactory
        get() = directiveWiringFactory
}
