package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import graphql.Scalars
import graphql.introspection.Introspection
import graphql.language.*
import graphql.schema.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberFunctions

private val log = KotlinLogging.logger {}

/**
 * Transforms the GraphQL schema by adding validation directives based on Jakarta validation annotations.
 * This makes validation constraints visible in the schema for API documentation purposes.
 */
@Component
class ValidationSchemaTransformer {

    fun transform(schema: GraphQLSchema, mutations: List<Mutation>, queries: List<Query>): GraphQLSchema {
        // Build a map of field -> argument -> validation directives
        val validationDirectivesMap = buildValidationDirectivesMap(mutations, queries)
        
        if (validationDirectivesMap.isEmpty()) {
            return schema
        }

        // Transform the schema to add validation directives
        return schema.transform { schemaBuilder ->
            // Add directive definitions if they don't exist
            val directiveDefinitions = mutableSetOf<GraphQLDirective>()
            
            if (schema.getDirective("notBlank") == null) {
                directiveDefinitions.add(createNotBlankDirective())
            }
            if (schema.getDirective("size") == null) {
                directiveDefinitions.add(createSizeDirective())
            }
            
            directiveDefinitions.forEach { schemaBuilder.additionalDirective(it) }
            
            // Transform Mutation type
            schema.mutationType?.let { mutationType ->
                val transformedMutation = transformObjectType(mutationType, validationDirectivesMap)
                schemaBuilder.mutation(transformedMutation)
            }
            
            // Transform Query type
            schema.queryType?.let { queryType ->
                val transformedQuery = transformObjectType(queryType, validationDirectivesMap)
                schemaBuilder.query(transformedQuery)
            }
            
            schemaBuilder
        }
    }

    private fun buildValidationDirectivesMap(
        mutations: List<Mutation>,
        queries: List<Query>
    ): Map<String, Map<String, List<ValidationDirective>>> {
        val result = mutableMapOf<String, Map<String, List<ValidationDirective>>>()
        
        val allComponents = (mutations + queries).map { it::class }
        
        for (kClass in allComponents) {
            for (function in kClass.memberFunctions) {
                val argumentDirectives = mutableMapOf<String, List<ValidationDirective>>()
                
                for (param in function.parameters) {
                    // Skip 'this' parameter
                    if (param.kind == KParameter.Kind.INSTANCE) continue
                    
                    val directives = mutableListOf<ValidationDirective>()
                    
                    param.annotations.forEach { annotation ->
                        when (annotation) {
                            is NotBlank -> directives.add(ValidationDirective.NotBlankDirective)
                            is Size -> directives.add(ValidationDirective.SizeDirective(annotation.max))
                        }
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
        validationDirectivesMap: Map<String, Map<String, List<ValidationDirective>>>
    ): GraphQLObjectType {
        return objectType.transform { typeBuilder ->
            val transformedFields = objectType.fieldDefinitions.map { field ->
                val argumentDirectives = validationDirectivesMap[field.name]
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
        argumentDirectives: Map<String, List<ValidationDirective>>
    ): GraphQLFieldDefinition {
        return field.transform { fieldBuilder ->
            val transformedArguments = field.arguments.map { argument ->
                val directives = argumentDirectives[argument.name]
                if (directives != null) {
                    transformArgument(argument, directives)
                } else {
                    argument
                }
            }
            fieldBuilder.clearArguments().arguments(transformedArguments)
        }
    }

    private fun transformArgument(
        argument: GraphQLArgument,
        directives: List<ValidationDirective>
    ): GraphQLArgument {
        return argument.transform { argBuilder ->
            directives.forEach { directive ->
                when (directive) {
                    is ValidationDirective.NotBlankDirective -> {
                        argBuilder.withAppliedDirective(
                            GraphQLAppliedDirective.newDirective()
                                .name("notBlank")
                                .build()
                        )
                    }
                    is ValidationDirective.SizeDirective -> {
                        argBuilder.withAppliedDirective(
                            GraphQLAppliedDirective.newDirective()
                                .name("size")
                                .argument(GraphQLAppliedDirectiveArgument.newArgument()
                                    .name("max")
                                    .valueLiteral(IntValue.newIntValue(directive.max.toBigInteger()).build())
                                    .type(Scalars.GraphQLInt)
                                    .build())
                                .build()
                        )
                    }
                }
            }
            argBuilder
        }
    }

    private fun createNotBlankDirective(): GraphQLDirective {
        return GraphQLDirective.newDirective()
            .name("notBlank")
            .description("Validates that the string is not null, empty, or blank")
            .validLocation(Introspection.DirectiveLocation.ARGUMENT_DEFINITION)
            .validLocation(Introspection.DirectiveLocation.INPUT_FIELD_DEFINITION)
            .build()
    }

    private fun createSizeDirective(): GraphQLDirective {
        return GraphQLDirective.newDirective()
            .name("size")
            .description("Validates the size of a string")
            .argument(GraphQLArgument.newArgument()
                .name("max")
                .type(GraphQLNonNull(Scalars.GraphQLInt))
                .description("Maximum size")
                .build())
            .validLocation(Introspection.DirectiveLocation.ARGUMENT_DEFINITION)
            .validLocation(Introspection.DirectiveLocation.INPUT_FIELD_DEFINITION)
            .build()
    }

    private sealed class ValidationDirective {
        object NotBlankDirective : ValidationDirective()
        data class SizeDirective(val max: Int) : ValidationDirective()
    }
}
