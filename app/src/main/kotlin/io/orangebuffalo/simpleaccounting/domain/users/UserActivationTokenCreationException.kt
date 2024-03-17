package io.orangebuffalo.simpleaccounting.domain.users

/**
 * A family of exceptions that are thrown when user activation token creation fails.
 */
sealed class UserActivationTokenCreationException(
    message: String
) : RuntimeException(message) {

    /**
     * Indicates that the user is already activated.
     */
    class UserAlreadyActivatedException(userId: Long) :
        UserActivationTokenCreationException("User $userId is already activated")
}
