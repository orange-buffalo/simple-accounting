package io.orangebuffalo.simpleaccounting.infra.graphql

import graphql.introspection.Introspection
import graphql.schema.*
import org.springframework.stereotype.Component

/**
 * Transforms the GraphQL schema by adding business error directives based on [BusinessError] annotations.
 * This makes business error information visible in the schema for API documentation purposes.
 * 
 * For each operation with @BusinessError annotations, this transformer:
 * 1. Creates a unique enum type containing all possible error codes for that operation
 * 2. Creates a corresponding @businessError directive applied to the field
 * 3. Generates the directive definition with the enum as argument type
 */
@Component
class BusinessErrorSchemaTransformer(
    private val businessErrorRegistry: BusinessErrorRegistry,
) {

    fun transform(schema: GraphQLSchema): GraphQLSchema {
        if (businessErrorRegistry.operationMappings.isEmpty()) {
            return schema
        }

        return schema.transform { schemaBuilder ->
            // Create and add the directive definition
            if (schema.getDirective(BUSINESS_ERROR_DIRECTIVE_NAME) == null) {
                schemaBuilder.additionalDirective(createBusinessErrorDirective())
            }

            // Transform mutation type to add directives to fields
            schema.mutationType?.let { mutationType ->
                schemaBuilder.mutation(transformObjectType(mutationType))
            }

            // Transform query type to add directives to fields
            schema.queryType?.let { queryType ->
                schemaBuilder.query(transformObjectType(queryType))
            }

            // Add the enum types to schema
            businessErrorRegistry.operationMappings.forEach { (operationName, errors) ->
                val enumTypeName = getEnumTypeName(operationName)
                val enumType = createErrorCodeEnum(enumTypeName, errors)
                schemaBuilder.additionalType(enumType)
            }

            schemaBuilder
        }
    }

    private fun transformObjectType(objectType: GraphQLObjectType): GraphQLObjectType {
        return objectType.transform { typeBuilder ->
            val transformedFields = objectType.fieldDefinitions.map { field ->
                val operationErrors = businessErrorRegistry.operationMappings[field.name]
                if (operationErrors != null) {
                    addBusinessErrorDirective(field, operationErrors)
                } else {
                    field
                }
            }
            typeBuilder.clearFields().fields(transformedFields)
        }
    }

    private fun addBusinessErrorDirective(
        field: GraphQLFieldDefinition,
        operationErrors: OperationBusinessErrors
    ): GraphQLFieldDefinition {
        val enumTypeName = getEnumTypeName(operationErrors.operationName)
        
        val appliedDirective = GraphQLAppliedDirective.newDirective()
            .name(BUSINESS_ERROR_DIRECTIVE_NAME)
            .argument(
                GraphQLAppliedDirectiveArgument.newArgument()
                    .name("errorCodes")
                    .type(GraphQLList.list(GraphQLTypeReference(enumTypeName)))
                    .valueLiteral(graphql.language.ArrayValue.newArrayValue()
                        .values(operationErrors.mappings.map { 
                            graphql.language.EnumValue.newEnumValue(it.errorCode).build() 
                        })
                        .build())
                    .build()
            )
            .build()

        return field.transform { fieldBuilder ->
            fieldBuilder.withAppliedDirective(appliedDirective)
        }
    }

    private fun createBusinessErrorDirective(): GraphQLDirective {
        return GraphQLDirective.newDirective()
            .name(BUSINESS_ERROR_DIRECTIVE_NAME)
            .description(
                "Declares business errors that can be returned by this operation. " +
                "When a business error occurs, the response will include an error with " +
                "extensions.errorType = 'BUSINESS_ERROR' and extensions.errorCode containing " +
                "one of the declared error codes."
            )
            .validLocation(Introspection.DirectiveLocation.FIELD_DEFINITION)
            .argument(
                GraphQLArgument.newArgument()
                    .name("errorCodes")
                    .description("List of possible business error codes for this operation.")
                    .type(GraphQLNonNull(GraphQLList.list(GraphQLNonNull(GraphQLTypeReference("String")))))
                    .build()
            )
            .build()
    }

    private fun createErrorCodeEnum(
        enumTypeName: String, 
        operationErrors: OperationBusinessErrors
    ): GraphQLEnumType {
        val builder = GraphQLEnumType.newEnum()
            .name(enumTypeName)
            .description("Possible business error codes for the ${operationErrors.operationName} operation.")

        operationErrors.mappings.forEach { mapping ->
            builder.value(
                GraphQLEnumValueDefinition.newEnumValueDefinition()
                    .name(mapping.errorCode)
                    .description(mapping.description.ifEmpty { "Error: ${mapping.errorCode}" })
                    .value(mapping.errorCode)
                    .build()
            )
        }

        return builder.build()
    }

    private fun getEnumTypeName(operationName: String): String {
        val capitalizedName = operationName.replaceFirstChar { it.uppercase() }
        return "${capitalizedName}ErrorCode"
    }

    companion object {
        const val BUSINESS_ERROR_DIRECTIVE_NAME = "businessError"
    }
}
