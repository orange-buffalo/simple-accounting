package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import io.orangebuffalo.simpleaccounting.business.oauthproviders.InvalidOAuthProviderScopeException
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProviderScope

private const val MAX_SCOPE_LENGTH = 255

/**
 * Validates the scopes submitted by an admin. Silently accepting malformed values would produce
 * authorization requests the providers reject, with no indication of the cause.
 */
internal fun List<String>.toProviderScopes(): Set<OAuthProviderScope> {
    forEach { scope ->
        if (scope.isBlank()) {
            throw InvalidOAuthProviderScopeException("must not be blank")
        }
        if (scope.length > MAX_SCOPE_LENGTH) {
            throw InvalidOAuthProviderScopeException("'${scope.take(20)}...' is longer than $MAX_SCOPE_LENGTH characters")
        }
        if (scope.any { it.isWhitespace() }) {
            throw InvalidOAuthProviderScopeException("'$scope' must not contain whitespace")
        }
    }
    val duplicate = groupBy { it }.entries.firstOrNull { it.value.size > 1 }
    if (duplicate != null) {
        throw InvalidOAuthProviderScopeException("'${duplicate.key}' is provided more than once")
    }
    return map { OAuthProviderScope(it) }.toSet()
}
