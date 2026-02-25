package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberFunctions

/**
 * Registry that collects and provides access to business error mappings from GraphQL operations.
 * 
 * This registry scans mutations and queries for [BusinessError] annotations and creates
 * a mapping structure that can be used by:
 * 1. [SaDataFetcherExceptionHandler] to handle exceptions at runtime
 * 2. [BusinessErrorSchemaTransformer] to generate dynamic enum types in the schema
 */
@Component
class BusinessErrorRegistry(
    mutations: List<Mutation>,
    queries: List<Query>,
) {
    /**
     * Maps operation name to its business error mappings.
     * Key is the operation name (GraphQL field name), value contains exception-to-error-code mappings.
     */
    val operationMappings: Map<String, OperationBusinessErrors> = scanOperations(mutations + queries)

    private fun scanOperations(operations: List<Any>): Map<String, OperationBusinessErrors> {
        val mappings = mutableMapOf<String, OperationBusinessErrors>()
        
        for (operation in operations) {
            for (function in operation::class.memberFunctions) {
                val businessErrors = function.findAnnotations<BusinessError>()
                if (businessErrors.isNotEmpty()) {
                    val errorMappings = businessErrors.map { annotation ->
                        BusinessErrorMapping(
                            exceptionClass = annotation.exceptionClass,
                            errorCode = annotation.errorCode,
                            description = annotation.description,
                            extensionsType = annotation.extensionsType.takeIf { it != Unit::class },
                        )
                    }
                    mappings[function.name] = OperationBusinessErrors(
                        operationName = function.name,
                        mappings = errorMappings
                    )
                }
            }
        }
        
        return mappings
    }

    /**
     * Finds the business error mapping for a given exception in a specific operation.
     * Returns null if no mapping is found.
     */
    fun findErrorMapping(operationName: String, exception: Throwable): BusinessErrorMapping? {
        val operationErrors = operationMappings[operationName] ?: return null
        return operationErrors.mappings.firstOrNull { mapping ->
            mapping.exceptionClass.isInstance(exception)
        }
    }
}

/**
 * Business error mappings for a single GraphQL operation.
 */
data class OperationBusinessErrors(
    val operationName: String,
    val mappings: List<BusinessErrorMapping>
)

/**
 * A single mapping from exception class to error code.
 */
data class BusinessErrorMapping(
    val exceptionClass: KClass<out Exception>,
    val errorCode: String,
    val description: String,
    val extensionsType: KClass<*>?,
)
