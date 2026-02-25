package io.orangebuffalo.simpleaccounting.business.api.errors

/**
 * Interface for exceptions that need to provide additional properties in GraphQL business error extensions.
 * When a [BusinessError]-annotated exception implements this interface, the additional extensions
 * will be merged into the GraphQL error response alongside the standard `errorType` and `errorCode`.
 *
 * The returned object should be an instance of the type declared in [BusinessError.extensionsType].
 * The object's properties will be serialized into the GraphQL error extensions map.
 */
interface GraphQlBusinessErrorExtensionsProvider {
    fun getBusinessErrorExtensions(): Any
}
