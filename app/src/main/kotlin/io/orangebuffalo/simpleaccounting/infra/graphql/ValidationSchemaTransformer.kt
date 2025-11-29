package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import graphql.Scalars
import graphql.introspection.Introspection
import graphql.language.IntValue
import graphql.schema.*
import jakarta.validation.ConstraintViolation
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberFunctions

/**
 * Registry entry that defines how a Jakarta validation annotation maps to a GraphQL directive
 * and how constraint violations are transformed into error responses.
 */
data class ValidationDirectiveMapping(
    val annotationClass: KClass<out Annotation>,
    val directiveName: String,
    val directiveDescription: String,
    val directiveArgumentsBuilder: (GraphQLDirective.Builder) -> Unit = {},
    val appliedDirectiveBuilder: (Annotation) -> GraphQLAppliedDirective,
    val errorCode: String,
    val paramsExtractor: ((ConstraintViolation<*>) -> Map<String, String>)? = null
)

/**
 * Single registry of all supported validation annotations with their directive mappings.
 * This provides cohesive definitions that are easy to track and understand.
 */
val validationDirectiveMappings: List<ValidationDirectiveMapping> = listOf(
    ValidationDirectiveMapping(
        annotationClass = NotBlank::class,
        directiveName = "notBlank",
        directiveDescription = "Validates that the string is not null, empty, or blank",
        appliedDirectiveBuilder = {
            GraphQLAppliedDirective.newDirective()
                .name("notBlank")
                .build()
        },
        errorCode = "MustNotBeBlank"
    ),
    ValidationDirectiveMapping(
        annotationClass = Size::class,
        directiveName = "size",
        directiveDescription = "Validates the size of a string",
        directiveArgumentsBuilder = { builder ->
            builder.argument(
                GraphQLArgument.newArgument()
                    .name("min")
                    .type(GraphQLNonNull(Scalars.GraphQLInt))
                    .description("Minimum size")
                    .build()
            ).argument(
                GraphQLArgument.newArgument()
                    .name("max")
                    .type(GraphQLNonNull(Scalars.GraphQLInt))
                    .description("Maximum size")
                    .build()
            )
        },
        appliedDirectiveBuilder = { annotation ->
            val size = annotation as Size
            GraphQLAppliedDirective.newDirective()
                .name("size")
                .argument(
                    GraphQLAppliedDirectiveArgument.newArgument()
                        .name("min")
                        .valueLiteral(IntValue.newIntValue(size.min.toBigInteger()).build())
                        .type(Scalars.GraphQLInt)
                        .build()
                )
                .argument(
                    GraphQLAppliedDirectiveArgument.newArgument()
                        .name("max")
                        .valueLiteral(IntValue.newIntValue(size.max.toBigInteger()).build())
                        .type(Scalars.GraphQLInt)
                        .build()
                )
                .build()
        },
        errorCode = "SizeConstraintViolated",
        paramsExtractor = { violation ->
            val attributes = violation.constraintDescriptor.attributes
            mapOf(
                "min" to (attributes["min"]?.toString() ?: "0"),
                "max" to (attributes["max"]?.toString() ?: "2147483647")
            )
        }
    )
)

private val mappingsByAnnotationClass: Map<KClass<out Annotation>, ValidationDirectiveMapping> =
    validationDirectiveMappings.associateBy { it.annotationClass }

/**
 * Transforms the GraphQL schema by adding validation directives based on Jakarta validation annotations.
 * This makes validation constraints visible in the schema for API documentation purposes.
 */
@Component
class ValidationSchemaTransformer {

    fun transform(schema: GraphQLSchema, mutations: List<Mutation>, queries: List<Query>): GraphQLSchema {
        val appliedDirectivesMap = buildAppliedDirectivesMap(mutations, queries)
        
        if (appliedDirectivesMap.isEmpty()) {
            return schema
        }

        val usedDirectiveNames = appliedDirectivesMap.values
            .flatMap { it.values }
            .flatten()
            .map { it.name }
            .toSet()

        return schema.transform { schemaBuilder ->
            // Add only the directive definitions that are actually used
            validationDirectiveMappings
                .filter { it.directiveName in usedDirectiveNames }
                .forEach { mapping ->
                    if (schema.getDirective(mapping.directiveName) == null) {
                        schemaBuilder.additionalDirective(createDirective(mapping))
                    }
                }
            
            schema.mutationType?.let { mutationType ->
                schemaBuilder.mutation(transformObjectType(mutationType, appliedDirectivesMap))
            }
            
            schema.queryType?.let { queryType ->
                schemaBuilder.query(transformObjectType(queryType, appliedDirectivesMap))
            }
            
            schemaBuilder
        }
    }

    private fun buildAppliedDirectivesMap(
        mutations: List<Mutation>,
        queries: List<Query>
    ): Map<String, Map<String, List<GraphQLAppliedDirective>>> {
        val result = mutableMapOf<String, Map<String, List<GraphQLAppliedDirective>>>()
        
        for (kClass in (mutations + queries).map { it::class }) {
            for (function in kClass.memberFunctions) {
                val argumentDirectives = mutableMapOf<String, List<GraphQLAppliedDirective>>()
                
                for (param in function.parameters) {
                    if (param.kind == KParameter.Kind.INSTANCE) continue
                    
                    val directives = param.annotations.mapNotNull { annotation ->
                        mappingsByAnnotationClass[annotation.annotationClass]
                            ?.appliedDirectiveBuilder
                            ?.invoke(annotation)
                    }
                    
                    if (directives.isNotEmpty()) {
                        argumentDirectives[param.name ?: continue] = directives
                    }
                }
                
                if (argumentDirectives.isNotEmpty()) {
                    result[function.name] = argumentDirectives
                }
            }
        }
        
        return result
    }

    private fun transformObjectType(
        objectType: GraphQLObjectType,
        appliedDirectivesMap: Map<String, Map<String, List<GraphQLAppliedDirective>>>
    ): GraphQLObjectType {
        return objectType.transform { typeBuilder ->
            val transformedFields = objectType.fieldDefinitions.map { field ->
                val argumentDirectives = appliedDirectivesMap[field.name]
                if (argumentDirectives != null) {
                    transformField(field, argumentDirectives)
                } else {
                    field
                }
            }
            typeBuilder.clearFields().fields(transformedFields)
        }
    }

    private fun transformField(
        field: GraphQLFieldDefinition,
        argumentDirectives: Map<String, List<GraphQLAppliedDirective>>
    ): GraphQLFieldDefinition {
        return field.transform { fieldBuilder ->
            val transformedArguments = field.arguments.map { argument ->
                val directives = argumentDirectives[argument.name]
                if (directives != null) {
                    argument.transform { argBuilder ->
                        directives.forEach { argBuilder.withAppliedDirective(it) }
                        argBuilder
                    }
                } else {
                    argument
                }
            }
            fieldBuilder.clearArguments().arguments(transformedArguments)
        }
    }

    private fun createDirective(mapping: ValidationDirectiveMapping): GraphQLDirective {
        return GraphQLDirective.newDirective()
            .name(mapping.directiveName)
            .description(mapping.directiveDescription)
            .validLocation(Introspection.DirectiveLocation.ARGUMENT_DEFINITION)
            .validLocation(Introspection.DirectiveLocation.INPUT_FIELD_DEFINITION)
            .apply { mapping.directiveArgumentsBuilder(this) }
            .build()
    }
}
