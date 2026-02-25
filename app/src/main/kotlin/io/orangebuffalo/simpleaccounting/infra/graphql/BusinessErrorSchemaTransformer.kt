package io.orangebuffalo.simpleaccounting.infra.graphql

import graphql.schema.*
import org.springframework.stereotype.Component

import kotlin.reflect.KClass
import kotlin.reflect.full.createType

/**
 * Transforms the GraphQL schema by adding dynamically generated enum types for business error codes.
 * 
 * For each operation with @BusinessError annotations, this transformer creates a unique enum type
 * named `<OperationName>ErrorCodes` containing all possible error codes for that operation.
 */
@Component
class BusinessErrorSchemaTransformer(
    private val businessErrorRegistry: BusinessErrorRegistry,
) {

    /**
     * Collects all unique extension types declared in business error mappings.
     * These types should be added to the schema's additional types during generation.
     */
    val extensionTypes: Set<KClass<*>>
        get() = businessErrorRegistry.operationMappings.values
            .flatMap { it.mappings }
            .mapNotNull { it.extensionsType }
            .toSet()

    fun transform(schema: GraphQLSchema): GraphQLSchema {
        if (businessErrorRegistry.operationMappings.isEmpty()) {
            return schema
        }

        return schema.transform { schemaBuilder ->
            // Add dynamically generated enum types for each operation
            businessErrorRegistry.operationMappings.forEach { (operationName, errors) ->
                val enumTypeName = getEnumTypeName(operationName)
                val enumType = createErrorCodeEnum(enumTypeName, operationName, errors)
                schemaBuilder.additionalType(enumType)
            }

            schemaBuilder
        }
    }

    private fun createErrorCodeEnum(
        enumTypeName: String, 
        operationName: String,
        operationErrors: OperationBusinessErrors
    ): GraphQLEnumType {
        val builder = GraphQLEnumType.newEnum()
            .name(enumTypeName)
            .description("Possible business error codes for the $operationName operation.")

        operationErrors.mappings.forEach { mapping ->
            builder.value(
                GraphQLEnumValueDefinition.newEnumValueDefinition()
                    .name(mapping.errorCode)
                    .description(mapping.description.ifEmpty { "Error code: ${mapping.errorCode}" })
                    .value(mapping.errorCode)
                    .build()
            )
        }

        return builder.build()
    }

    private fun getEnumTypeName(operationName: String): String {
        val capitalizedName = operationName.replaceFirstChar { it.uppercase() }
        return "${capitalizedName}ErrorCodes"
    }
}
