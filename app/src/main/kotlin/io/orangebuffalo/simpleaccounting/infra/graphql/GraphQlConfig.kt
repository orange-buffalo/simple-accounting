package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import com.expediagroup.graphql.generator.federation.directives.ContactDirective
import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.expediagroup.graphql.server.Schema
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.infra.graphql.RequiredAuth.AuthType
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

/**
 * A query that exposes cross-cutting concern types to the GraphQL schema.
 * This query is not meant to be called by clients, but rather to ensure
 * that certain types are included in the generated schema for type safety.
 */
@Component
class SaCrossCuttingConcernsQuery : Query {
    
    @Suppress("unused")
    @GraphQLDescription(
        "Internal query used to expose cross-cutting concern types in the schema. " +
        "This query should not be called by clients. " +
        "It exists solely to ensure type definitions are available for error handling and other infrastructure concerns."
    )
    @RequiredAuth(AuthType.ADMIN_USER)
    @Deprecated("This query is for schema generation only and should not be called.", level = DeprecationLevel.ERROR)
    fun schemaTypes(): SchemaTypes {
        throw UnsupportedOperationException("This query is for schema generation only and should not be called.")
    }
    
    @GraphQLDescription("Container for cross-cutting concern types exposed in the schema.")
    data class SchemaTypes(
        @param:GraphQLDescription("Error types that can be returned in GraphQL error extensions.")
        val errorType: SaGrapQlErrorType
    )
}
