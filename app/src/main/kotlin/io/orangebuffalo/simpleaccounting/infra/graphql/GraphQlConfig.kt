package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelNames
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import com.expediagroup.graphql.generator.execution.KotlinDataFetcherFactoryProvider
import com.expediagroup.graphql.generator.federation.directives.ContactDirective
import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.expediagroup.graphql.server.Schema
import com.expediagroup.graphql.server.spring.GraphQLConfigurationProperties
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLType
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.Optional

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
 * This overrides the default auto-configuration to inject additional types into the schema.
 */
@Configuration
class SaGraphQlSchemaConfig {
    
    /**
     * Provides the SchemaGeneratorConfig with additional types for cross-cutting concerns.
     * This includes error type enums and other infrastructure types that need to be
     * exposed in the schema for type-safe client code generation.
     */
    @Bean
    @ConditionalOnMissingBean
    fun schemaConfig(
        config: GraphQLConfigurationProperties,
        topLevelNames: Optional<TopLevelNames>,
        hooks: Optional<SchemaGeneratorHooks>,
        dataFetcherFactoryProvider: KotlinDataFetcherFactoryProvider,
    ): SchemaGeneratorConfig {
        val saGrapQlErrorTypeEnum = GraphQLEnumType.newEnum()
            .name("SaGrapQlErrorType")
            .description(
                "Defines the error types that can be returned in GraphQL errors. " +
                "These error types are included in the `extensions.errorType` field of GraphQL errors."
            )
            .value(
                "NOT_AUTHORIZED",
                SaGrapQlErrorType.NOT_AUTHORIZED,
                "Indicates that the request requires authentication or the user is not authorized to perform the operation."
            )
            .build()
        
        val additionalTypes: Set<GraphQLType> = setOf(saGrapQlErrorTypeEnum)
        
        return SchemaGeneratorConfig(
            supportedPackages = config.packages,
            topLevelNames = topLevelNames.orElse(TopLevelNames()),
            hooks = hooks.orElseGet { com.expediagroup.graphql.generator.hooks.NoopSchemaGeneratorHooks },
            dataFetcherFactoryProvider = dataFetcherFactoryProvider,
            additionalTypes = additionalTypes
        )
    }
}
