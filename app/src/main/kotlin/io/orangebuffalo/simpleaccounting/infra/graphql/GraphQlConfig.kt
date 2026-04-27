package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.generator.SchemaGenerator
import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.directives.DirectiveMetaInformation
import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import com.expediagroup.graphql.generator.hooks.FlowSubscriptionSchemaGeneratorHooks
import com.expediagroup.graphql.server.Schema
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import graphql.Scalars
import graphql.introspection.Introspection
import graphql.language.IntValue
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLAppliedDirectiveArgument
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeReference
import io.orangebuffalo.simpleaccounting.business.api.directives.REQUIRED_AUTH_DIRECTIVE_NAME
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
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

@GraphQLDescription("GraphQL schema for Simple Accounting application")
@Component
class SaGraphQlSchema : Schema

@Component
class SaSchemaGeneratorHooks : FlowSubscriptionSchemaGeneratorHooks() {
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

    // Kotlin 2.3.21 introduced a regression in kotlin-reflect where accessing annotations
    // on constructor parameters of annotation classes throws KotlinReflectionInternalError.
    // graphql-kotlin uses kotlin-reflect to inspect annotation class constructor parameters
    // when building directive definitions and applied directives. By overriding these hooks,
    // we use Java reflection instead, bypassing the broken code path entirely.

    override fun willGenerateDirective(directiveMetaInformation: DirectiveMetaInformation): GraphQLDirective? {
        if (!directiveMetaInformation.directive.annotationClass.java.isAnnotation) return null

        val graphqlDirectiveAnnotation = directiveMetaInformation.directiveAnnotation
        val builder = GraphQLDirective.newDirective()
            .name(directiveMetaInformation.effectiveName)
            .repeatable(directiveMetaInformation.repeatable)

        if (graphqlDirectiveAnnotation.description.isNotEmpty()) {
            builder.description(graphqlDirectiveAnnotation.description)
        }
        graphqlDirectiveAnnotation.locations.forEach { builder.validLocation(it) }

        directiveMetaInformation.directive.annotationClass.java.declaredMethods
            .sortedBy { it.name }
            .forEach { method ->
                if (method.isAnnotationPresent(GraphQLIgnore::class.java)) return@forEach
                val graphqlType = javaTypeToGraphQLInputType(method.returnType) ?: return@forEach
                builder.argument(GraphQLArgument.newArgument().name(method.name).type(graphqlType).build())
            }

        return builder.build()
    }

    override fun willApplyDirective(
        directiveMetaInformation: DirectiveMetaInformation,
        directive: GraphQLDirective,
    ): GraphQLAppliedDirective? {
        if (!directiveMetaInformation.directive.annotationClass.java.isAnnotation) return null

        return directive.toAppliedDirective().transform { appliedBuilder ->
            directiveMetaInformation.directive.annotationClass.java.declaredMethods
                .forEach { method ->
                    if (method.isAnnotationPresent(GraphQLIgnore::class.java)) return@forEach
                    val argDef = directive.getArgument(method.name) ?: return@forEach
                    val value = method.invoke(directiveMetaInformation.directive)
                    appliedBuilder.argument(argDef.toAppliedArgument().transform { it.valueProgrammatic(value) })
                }
        }
    }

    private fun javaTypeToGraphQLInputType(javaType: Class<*>): GraphQLInputType? = when {
        javaType == String::class.java -> GraphQLNonNull.nonNull(Scalars.GraphQLString) as GraphQLInputType
        javaType == Boolean::class.java || javaType == java.lang.Boolean.TYPE ->
            GraphQLNonNull.nonNull(Scalars.GraphQLBoolean) as GraphQLInputType
        javaType == Int::class.java || javaType == java.lang.Integer.TYPE ->
            GraphQLNonNull.nonNull(Scalars.GraphQLInt) as GraphQLInputType
        javaType.isEnum -> GraphQLNonNull.nonNull(GraphQLTypeReference.typeRef(javaType.simpleName)) as GraphQLInputType
        else -> null
    }

    override fun didBuildSchema(builder: GraphQLSchema.Builder): GraphQLSchema.Builder =
        connectionSchemaGenerationSupport.addEdgeTypesToSchema(builder)
            // @ContactDirective is from a third-party library (graphql-kotlin-federation), so we cannot
            // apply the willGenerateDirective/willApplyDirective hooks approach used for our own directives.
            // We removed the @ContactDirective annotation from SaGraphQlSchema and add it programmatically here.
            .additionalDirective(contactDirectiveType)
            .withSchemaAppliedDirective(
                contactDirectiveType.toAppliedDirective().transform { appliedDirectiveBuilder ->
                    appliedDirectiveBuilder.argument(
                        contactDirectiveType.getArgument("description")
                            .toAppliedArgument().transform {
                                it.valueProgrammatic("For any questions, issues or feature requests, please open an issue or discussion on GitHub.")
                            }
                    )
                    appliedDirectiveBuilder.argument(
                        contactDirectiveType.getArgument("name")
                            .toAppliedArgument().transform { it.valueProgrammatic("Simple Accounting") }
                    )
                    appliedDirectiveBuilder.argument(
                        contactDirectiveType.getArgument("url")
                            .toAppliedArgument().transform {
                                it.valueProgrammatic("https://github.com/orange-buffalo/simple-accounting/issues")
                            }
                    )
                }
            )
}

private const val CONTACT_DIRECTIVE_NAME_VALUE = "contact"

private val contactDirectiveType: GraphQLDirective = GraphQLDirective.newDirective()
    .name(CONTACT_DIRECTIVE_NAME_VALUE)
    .description("Provides contact information of the owner responsible for this subgraph schema.")
    .validLocations(Introspection.DirectiveLocation.SCHEMA)
    .argument(GraphQLArgument.newArgument().name("description").type(GraphQLNonNull.nonNull(Scalars.GraphQLString) as graphql.schema.GraphQLInputType).build())
    .argument(GraphQLArgument.newArgument().name("name").type(GraphQLNonNull.nonNull(Scalars.GraphQLString) as graphql.schema.GraphQLInputType).build())
    .argument(GraphQLArgument.newArgument().name("url").type(GraphQLNonNull.nonNull(Scalars.GraphQLString) as graphql.schema.GraphQLInputType).build())
    .build()

private val GraphQLLongScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("Long")
    .description("A 64-bit signed integer.")
    .coercing(object : graphql.schema.Coercing<Long, Long> {
        override fun serialize(input: Any, graphQLContext: graphql.GraphQLContext, locale: Locale): Long =
            when (input) {
                is Long -> input
                is Number -> input.toLong()
                is String -> input.toLong()
                else -> throw CoercingSerializeException("Expected a Long but got: $input")
            }

        override fun parseValue(input: Any, graphQLContext: graphql.GraphQLContext, locale: Locale): Long =
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
            locale: Locale
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
        override fun serialize(input: Any, graphQLContext: graphql.GraphQLContext, locale: Locale): String =
            when (input) {
                is Instant -> input.toString()
                else -> throw CoercingSerializeException("Expected an Instant but got: $input")
            }

        override fun parseValue(input: Any, graphQLContext: graphql.GraphQLContext, locale: Locale): Instant =
            when (input) {
                is String -> Instant.parse(input)
                else -> throw CoercingParseValueException("Expected a String for DateTime but got: $input")
            }

        override fun parseLiteral(
            input: Value<*>,
            variables: graphql.execution.CoercedVariables,
            graphQLContext: graphql.GraphQLContext,
            locale: Locale
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
        override fun serialize(input: Any, graphQLContext: graphql.GraphQLContext, locale: Locale): String =
            when (input) {
                is LocalDate -> input.toString()
                else -> throw CoercingSerializeException("Expected a LocalDate but got: $input")
            }

        override fun parseValue(input: Any, graphQLContext: graphql.GraphQLContext, locale: Locale): LocalDate =
            when (input) {
                is String -> LocalDate.parse(input)
                else -> throw CoercingParseValueException("Expected a String for LocalDate but got: $input")
            }

        override fun parseLiteral(
            input: Value<*>,
            variables: graphql.execution.CoercedVariables,
            graphQLContext: graphql.GraphQLContext,
            locale: Locale
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
                    // AuthType is only referenced via @auth directive arguments. Since willGenerateDirective
                    // uses Java reflection (bypassing graphql-kotlin's type registration), we register it explicitly.
                    RequiredAuth.AuthType::class.createType(),
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
