package io.orangebuffalo.simpleaccounting.business.oauthproviders

/**
 * The requested scopes are sent to the authorization server as a single whitespace delimited
 * string, so their content is constrained.
 */
class InvalidOAuthProviderScopeException(reason: String) : RuntimeException("Invalid scope: $reason")
