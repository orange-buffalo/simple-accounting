package io.orangebuffalo.simpleaccounting.infra.oauth2

/**
 * Thrown when the OAuth2 authorization flow fails for any reason.
 * Mapped to the `AUTHORIZATION_FAILED` business error code in the GraphQL API.
 */
class OAuth2AuthorizationFlowException(message: String, cause: Throwable) : Exception(message, cause)
