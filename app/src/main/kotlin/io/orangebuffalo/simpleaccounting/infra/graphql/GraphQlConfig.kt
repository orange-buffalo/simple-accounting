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
import graphql.schema.GraphQLSchema
import io.orangebuffalo.simpleaccounting.business.api.directives.REQUIRED_AUTH_DIRECTIVE_NAME
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuthDirectiveWiring
import io.orangebuffalo.simpleaccounting.business.api.errors.SaGrapQlErrorType
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorCode
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorDetails
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorParam
import org.springframework.aop.framework.Advised
import org.springframework.aop.support.AopUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.*
import kotlin.reflect.full.createType

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
class SaGraphQlSchemaConfig {

    /**
     * Full copy of [com.expediagroup.graphql.server.spring.NonFederatedSchemaAutoConfiguration.schema] but with
     * additional types added and validation directives transformation.
     */
    @Bean
    fun schema(
        queries: Optional<List<Query>>,
        mutations: Optional<List<Mutation>>,
        subscriptions: Optional<List<Subscription>>,
        schemaConfig: SchemaGeneratorConfig,
        schemaObject: Optional<Schema>,
        validationSchemaTransformer: ValidationSchemaTransformer,
        businessErrorSchemaTransformer: BusinessErrorSchemaTransformer,
    ): GraphQLSchema {
        val generator = SchemaGenerator(config = schemaConfig)
        
        val baseSchema = generator.use {
            it.generateSchema(
                queries = queries.orElse(emptyList()).toTopLevelObjects(),
                mutations = mutations.orElse(emptyList()).toTopLevelObjects(),
                subscriptions = subscriptions.orElse(emptyList()).toTopLevelObjects(),
                schemaObject = schemaObject.orElse(null)?.toTopLevelObject(),
                additionalTypes = setOf(
                    SaGrapQlErrorType::class.createType(),
                    ValidationErrorCode::class.createType(),
                    ValidationErrorDetails::class.createType(),
                    ValidationErrorParam::class.createType(),
                ) + businessErrorSchemaTransformer.extensionTypes.map { it.createType() }
            )
        }
        
        // Transform schema to add validation directives based on Jakarta annotations
        val schemaWithValidation = validationSchemaTransformer.transform(
            baseSchema,
            mutations.orElse(emptyList()),
            queries.orElse(emptyList())
        )
        
        // Transform schema to add dynamically generated business error enum types
        return businessErrorSchemaTransformer.transform(schemaWithValidation)
    }

    private fun List<Any>.toTopLevelObjects(): List<TopLevelObject> = this.map {
        it.toTopLevelObject()
    }

    private fun Any.toTopLevelObject(): TopLevelObject = this.let {
        val klazz = if (AopUtils.isAopProxy(it) && it is Advised) {
            it.targetSource.target!!::class
        } else {
            it::class
        }
        TopLevelObject(it, klazz)
    }
}
