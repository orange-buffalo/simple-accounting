package io.orangebuffalo.simpleaccounting.domain.users

sealed class UserUpdateException(message: String) : RuntimeException(message) {

    /**
     * Exception thrown when user with the same name already exists.
     */
    class UserAlreadyExistsException(userName: String) :
        UserUpdateException("User with name '$userName' already exists")
}
