package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.generator.GraphQLTypeResolver
import com.expediagroup.graphql.generator.SchemaGenerator
import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelNames
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import com.expediagroup.graphql.generator.execution.KotlinDataFetcherFactoryProvider
import com.expediagroup.graphql.generator.federation.directives.ContactDirective
import com.expediagroup.graphql.generator.hooks.NoopSchemaGeneratorHooks
import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.expediagroup.graphql.server.Schema
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import com.expediagroup.graphql.server.spring.GraphQLConfigurationProperties
import graphql.schema.GraphQLSchema
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.Optional
import kotlin.reflect.KType

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
 */
@Configuration
class SaGraphQlSchemaConfig(
    private val config: GraphQLConfigurationProperties
) {
    
    @Bean
    @ConditionalOnMissingBean
    fun schemaConfig(
        topLevelNames: Optional<TopLevelNames>,
        hooks: Optional<SchemaGeneratorHooks>,
        dataFetcherFactoryProvider: KotlinDataFetcherFactoryProvider,
        typeResolver: GraphQLTypeResolver
    ): SchemaGeneratorConfig = SchemaGeneratorConfig(
        supportedPackages = config.packages,
        topLevelNames = topLevelNames.orElse(TopLevelNames()),
        hooks = hooks.orElse(NoopSchemaGeneratorHooks),
        dataFetcherFactoryProvider = dataFetcherFactoryProvider,
        introspectionEnabled = config.introspection.enabled,
        typeResolver = typeResolver
    )
    
    /**
     * Overrides the default schema bean to include additional types for introspection.
     */
    @Bean
    @ConditionalOnMissingBean
    fun schema(
        queries: Optional<List<Query>>,
        mutations: Optional<List<Mutation>>,
        subscriptions: Optional<List<Subscription>>,
        schemaConfig: SchemaGeneratorConfig,
        schemaObject: Optional<Schema>,
        additionalTypes: Optional<Set<KType>>
    ): GraphQLSchema {
        val generator = SchemaGenerator(schemaConfig)
        return generator.generateSchema(
            queries = queries.orElse(emptyList()).map { TopLevelObject(it) },
            mutations = mutations.orElse(emptyList()).map { TopLevelObject(it) },
            subscriptions = subscriptions.orElse(emptyList()).map { TopLevelObject(it) },
            additionalTypes = additionalTypes.orElse(emptySet()),
            additionalInputTypes = emptySet(),
            schemaObject = schemaObject.orElse(null)?.let { TopLevelObject(it) }
        )
    }
    
    /**
     * Provides additional KTypes for introspection to include cross-cutting concern types in the schema.
     */
    @Bean
    @ConditionalOnMissingBean(name = ["graphQLAdditionalTypes"])
    fun graphQLAdditionalTypes(): Set<KType> = setOf(
        kotlin.reflect.typeOf<SaGrapQlErrorType>()
    )
}
