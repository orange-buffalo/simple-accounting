package io.orangebuffalo.simpleaccounting.domain.users

sealed class UserCreationException(message: String) : RuntimeException(message) {

    /**
     * Exception thrown when user with the same name already exists.
     */
    class UserAlreadyExistsException(userName: String) :
        UserCreationException("User with name '$userName' already exists")
}
