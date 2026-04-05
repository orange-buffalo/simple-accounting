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
import graphql.language.IntValue
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import io.orangebuffalo.simpleaccounting.business.api.directives.REQUIRED_AUTH_DIRECTIVE_NAME
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuthDirectiveWiring
import io.orangebuffalo.simpleaccounting.business.api.errors.SaGrapQlErrorType
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorCode
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorDetails
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorParam
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionSchemaGenerationSupport
import org.springframework.aop.framework.Advised
import org.springframework.aop.support.AopUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KType
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

    val connectionSchemaGenerationSupport = ConnectionSchemaGenerationSupport()

    override fun willGenerateGraphQLType(type: KType): GraphQLType? {
        if (type.classifier == Long::class) return GraphQLLongScalar
        if (type.classifier == LocalDate::class) return GraphQLLocalDateScalar
        if (type.classifier == Instant::class) return GraphQLDateTimeScalar
        return connectionSchemaGenerationSupport.willGenerateGraphQLType(type)
    }

    override fun didBuildSchema(builder: GraphQLSchema.Builder): GraphQLSchema.Builder =
        connectionSchemaGenerationSupport.addEdgeTypesToSchema(builder)
}

private val GraphQLLongScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("Long")
    .description("A 64-bit signed integer.")
    .coercing(object : graphql.schema.Coercing<Long, Long> {
        override fun serialize(input: Any, graphQLContext: graphql.GraphQLContext, locale: java.util.Locale): Long =
            when (input) {
                is Long -> input
                is Number -> input.toLong()
                is String -> input.toLong()
                else -> throw CoercingSerializeException("Expected a Long but got: $input")
            }

        override fun parseValue(input: Any, graphQLContext: graphql.GraphQLContext, locale: java.util.Locale): Long =
            when (input) {
                is Long -> input
                is Number -> input.toLong()
                is String -> input.toLong()
                else -> throw CoercingParseValueException("Expected a Long but got: $input")
            }

        override fun parseLiteral(
            input: Value<*>,
            variables: graphql.execution.CoercedVariables,
            graphQLContext: graphql.GraphQLContext,
            locale: java.util.Locale
        ): Long = when (input) {
            is IntValue -> input.value.toLong()
            is StringValue -> input.value.toLong()
            else -> throw CoercingParseLiteralException("Expected IntValue or StringValue but got: $input")
        }
    })
    .build()

private val GraphQLDateTimeScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("DateTime")
    .description("A date-time instant, serialized as an ISO-8601 string (e.g. '2025-01-15T10:30:00Z').")
    .coercing(object : graphql.schema.Coercing<Instant, String> {
        override fun serialize(input: Any, graphQLContext: graphql.GraphQLContext, locale: java.util.Locale): String =
            when (input) {
                is Instant -> input.toString()
                else -> throw CoercingSerializeException("Expected an Instant but got: $input")
            }

        override fun parseValue(input: Any, graphQLContext: graphql.GraphQLContext, locale: java.util.Locale): Instant =
            when (input) {
                is String -> Instant.parse(input)
                else -> throw CoercingParseValueException("Expected a String for DateTime but got: $input")
            }

        override fun parseLiteral(
            input: Value<*>,
            variables: graphql.execution.CoercedVariables,
            graphQLContext: graphql.GraphQLContext,
            locale: java.util.Locale
        ): Instant = when (input) {
            is StringValue -> Instant.parse(input.value)
            else -> throw CoercingParseLiteralException("Expected StringValue for DateTime but got: $input")
        }
    })
    .build()

private val GraphQLLocalDateScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("LocalDate")
    .description("A date without time, serialized as an ISO-8601 string (e.g. '2025-01-15').")
    .coercing(object : graphql.schema.Coercing<LocalDate, String> {
        override fun serialize(input: Any, graphQLContext: graphql.GraphQLContext, locale: java.util.Locale): String =
            when (input) {
                is LocalDate -> input.toString()
                else -> throw CoercingSerializeException("Expected a LocalDate but got: $input")
            }

        override fun parseValue(input: Any, graphQLContext: graphql.GraphQLContext, locale: java.util.Locale): LocalDate =
            when (input) {
                is String -> LocalDate.parse(input)
                else -> throw CoercingParseValueException("Expected a String for LocalDate but got: $input")
            }

        override fun parseLiteral(
            input: Value<*>,
            variables: graphql.execution.CoercedVariables,
            graphQLContext: graphql.GraphQLContext,
            locale: java.util.Locale
        ): LocalDate = when (input) {
            is StringValue -> LocalDate.parse(input.value)
            else -> throw CoercingParseLiteralException("Expected StringValue for LocalDate but got: $input")
        }
    })
    .build()

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
        schemaGeneratorHooks: SaSchemaGeneratorHooks,
    ): GraphQLSchema {
        val generator = SchemaGenerator(config = schemaConfig)

        val allOperations = queries.orElse(emptyList()) + mutations.orElse(emptyList())
        val connectionNodeTypes = schemaGeneratorHooks.connectionSchemaGenerationSupport
            .collectAdditionalTypes(allOperations)
        
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
                  + connectionNodeTypes
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
