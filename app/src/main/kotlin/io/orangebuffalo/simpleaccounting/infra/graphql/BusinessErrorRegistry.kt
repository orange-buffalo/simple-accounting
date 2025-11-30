package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberFunctions

/**
 * Registry that collects and provides access to business error mappings from GraphQL operations.
 * 
 * This registry scans mutations and queries for [BusinessError] annotations and creates
 * a mapping structure that can be used by [SaDataFetcherExceptionHandler] to handle exceptions at runtime.
 */
@Component
class BusinessErrorRegistry(
    mutations: List<Mutation>,
    queries: List<Query>,
) {
    private val scanResult = scanOperations(mutations + queries)
    
    /**
     * Maps operation name to its business error mappings.
     * Key is the operation name (GraphQL field name), value contains exception-to-error-code mappings.
     */
    val operationMappings: Map<String, OperationBusinessErrors> = scanResult.first
    
    /**
     * Set of all unique error code enum types used across all operations.
     * These types should be added to the GraphQL schema's additionalTypes.
     */
    val errorCodeEnumTypes: Set<KType> = scanResult.second

    private fun scanOperations(operations: List<Any>): Pair<Map<String, OperationBusinessErrors>, Set<KType>> {
        val mappings = mutableMapOf<String, OperationBusinessErrors>()
        val enumTypes = mutableSetOf<KType>()
        
        for (operation in operations) {
            for (function in operation::class.memberFunctions) {
                val businessErrors = function.findAnnotations<BusinessError>()
                if (businessErrors.isNotEmpty()) {
                    val errorMappings = businessErrors.map { annotation ->
                        enumTypes.add(annotation.errorCodeClass.createType())
                        BusinessErrorMapping(
                            exceptionClass = annotation.exceptionClass,
                            errorCode = annotation.errorCode,
                        )
                    }
                    mappings[function.name] = OperationBusinessErrors(
                        operationName = function.name,
                        mappings = errorMappings
                    )
                }
            }
        }
        
        return Pair(mappings, enumTypes)
    }

    /**
     * Finds the error code for a given exception in a specific operation.
     * Returns null if no mapping is found.
     */
    fun findErrorCode(operationName: String, exception: Throwable): String? {
        val operationErrors = operationMappings[operationName] ?: return null
        return operationErrors.mappings.firstOrNull { mapping ->
            mapping.exceptionClass.isInstance(exception)
        }?.errorCode
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
)
