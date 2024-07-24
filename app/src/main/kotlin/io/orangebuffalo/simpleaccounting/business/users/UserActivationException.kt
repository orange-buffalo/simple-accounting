package io.orangebuffalo.simpleaccounting.business.users

/**
 * A family of exceptions that are thrown when user activation fails.
 */
sealed class UserActivationException(messages: String) : RuntimeException(messages) {

    /**
     * Indicates that the token used for activation is expired.
     */
    class TokenExpiredException : UserActivationException("Token expired")
}
