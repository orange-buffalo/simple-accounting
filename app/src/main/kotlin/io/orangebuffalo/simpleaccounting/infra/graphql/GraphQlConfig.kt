package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.generator.GraphQLTypeResolver
import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelNames
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import com.expediagroup.graphql.generator.execution.KotlinDataFetcherFactoryProvider
import com.expediagroup.graphql.generator.federation.directives.ContactDirective
import com.expediagroup.graphql.generator.hooks.NoopSchemaGeneratorHooks
import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.expediagroup.graphql.server.Schema
import com.expediagroup.graphql.server.spring.GraphQLConfigurationProperties
import graphql.schema.GraphQLEnumType
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
 * Configuration for GraphQL schema generation.
 */
@Configuration
class SaGraphQlSchemaConfig(
    private val config: GraphQLConfigurationProperties
) {
    
    @Bean
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
        additionalTypes = setOf(
            GraphQLEnumType.newEnum()
                .name("SaGrapQlErrorType")
                .description("Defines the error types that can be returned in GraphQL errors. These error types are included in the `extensions.errorType` field of GraphQL errors.")
                .value("NOT_AUTHORIZED", SaGrapQlErrorType.NOT_AUTHORIZED, "Indicates that the request requires authentication or the user is not authorized to perform the operation.")
                .build()
        ),
        typeResolver = typeResolver
    )
}
