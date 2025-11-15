package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.generator.SchemaGenerator
import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import com.expediagroup.graphql.generator.federation.directives.ContactDirective
import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.expediagroup.graphql.server.Schema
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import com.expediagroup.graphql.server.spring.execution.SpringGraphQLContextFactory
import graphql.schema.GraphQLSchema
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.util.Optional
import kotlin.reflect.KType
import kotlin.reflect.typeOf

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
 * Configuration for GraphQL schema generation that includes additional cross-cutting concern types.
 * Provides additional KTypes to the schema generator for introspection.
 */
@Configuration
class SaGraphQlSchemaConfig {
    
    /**
     * Overrides the default schema bean to include additional types for introspection.
     * This allows cross-cutting concern types like error enums to be exposed in the schema
     * without being directly referenced in queries or mutations.
     */
    @Bean
    @Primary
    fun schema(
        queries: Optional<List<Query>>,
        mutations: Optional<List<Mutation>>,
        subscriptions: Optional<List<Subscription>>,
        schemaConfig: SchemaGeneratorConfig,
        schemaObject: Optional<Schema>,
    ): GraphQLSchema {
        val generator = SchemaGenerator(schemaConfig)
        
        val additionalTypes: Set<KType> = setOf(
            typeOf<SaGrapQlErrorType>()
        )
        
        return generator.generateSchema(
            queries = queries.orElse(emptyList()).map { TopLevelObject(it) },
            mutations = mutations.orElse(emptyList()).map { TopLevelObject(it) },
            subscriptions = subscriptions.orElse(emptyList()).map { TopLevelObject(it) },
            additionalTypes = additionalTypes,
            additionalInputTypes = emptySet(),
            schemaObject = schemaObject.orElse(null)?.let { TopLevelObject(it) }
        )
    }
}
