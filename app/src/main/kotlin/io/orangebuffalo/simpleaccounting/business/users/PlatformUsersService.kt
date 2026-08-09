package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.security.authentication.AuthenticationService
import io.orangebuffalo.simpleaccounting.business.security.ensureRegularUserPrincipal
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.ObjectProvider
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

/**
 * Service for managing platform users, including their activation tokens.
 */
@Service
class PlatformUsersService(
    private val userRepository: PlatformUsersRepository,
    private val userActivationTokensRepository: UserActivationTokensRepository,
    private val userManagementProperties: UserManagementProperties,
    private val timeService: TimeService,
    // object provider is used to avoid circular dependencies
    private val authenticationService: ObjectProvider<AuthenticationService>,
) {

    fun getCurrentUser(): PlatformUser =
        userRepository.findByUserName(ensureRegularUserPrincipal().userName)
            ?: throw IllegalStateException("Current principal is not resolved to a user")

    fun getUserByUserName(userName: String): PlatformUser? = userRepository.findByUserName(userName)

    fun save(user: PlatformUser): PlatformUser = userRepository.save(user)

    /**
     * Validates and saves updated user data.
     * @throws UserUpdateException.UserAlreadyExistsException in case another user with the same name already exists
     */
    fun updateUser(updatedUserData: PlatformUser): PlatformUser {
        check(updatedUserData.id != null) {
            "User id must be provided for update"
        }
        val existingUser = userRepository.findByUserName(updatedUserData.userName)
        if (existingUser != null && existingUser.id != updatedUserData.id) {
            throw UserUpdateException.UserAlreadyExistsException(updatedUserData.userName)
        }
        return userRepository.save(updatedUserData)
    }

    /**
     * Creates a new user.
     * @throws UserCreationException.UserAlreadyExistsException in case another user with the same name already exists
     */
    fun createUser(
        userName: String,
        isAdmin: Boolean,
    ): PlatformUser {
        val existingUser = userRepository.findByUserName(userName)
        if (existingUser != null) {
            throw UserCreationException.UserAlreadyExistsException(userName)
        }

        val user = save(
            PlatformUser(
                userName = userName,
                passwordHash = RandomStringUtils.secure().next(100),
                isAdmin = isAdmin,
                i18nSettings = I18nSettings(locale = "en_AU", language = "en"),
                activated = false
            )
        )
        setupUserActivationToken(user.id!!)
        return user
    }

    fun getUserByUserId(userId: String): PlatformUser =
        userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User $userId is not found") }

    /**
     * Retrieves user activation token by user id. There is always at most one token per user.
     * In case the token is not found, or is expired, null is returned.
     * In case the token is expired, it is removed.
     */
    fun getUserActivationTokenForUser(userId: String): UserActivationToken? {
        val token = userActivationTokensRepository.findByUserId(userId)
        return ensureTokenIsNotExpired(token)
    }

    private fun ensureTokenIsNotExpired(token: UserActivationToken?): UserActivationToken? {
        if (token == null) return null
        if (token.expired) {
            userActivationTokensRepository.delete(token)
            return null
        }
        return token
    }

    /**
     * Retrieves user activation token by token value.
     * In case the token is not found, or is expired, null is returned.
     * In case the token is expired, it is removed.
     */
    fun getUserActivationToken(tokenValue: String): UserActivationToken? {
        val token = userActivationTokensRepository.findByToken(tokenValue)
        return ensureTokenIsNotExpired(token)
    }

    /**
     * Creates a new user activation token for the user with the specified id.
     * In case there is an existing token for the user, it is replaced with a new one.
     */
    fun createUserActivationToken(userId: String): UserActivationToken {
        val user = userRepository.findByIdOrNull(userId)
        if (user == null) {
            throw EntityNotFoundException("User $userId is not found")
        }
        if (user.activated) {
            throw UserActivationTokenCreationException.UserAlreadyActivatedException(userId)
        }
        val token = userActivationTokensRepository.findByUserId(userId)
        if (token != null) {
            userActivationTokensRepository.delete(token)
        }
        return setupUserActivationToken(userId)
    }

    private fun setupUserActivationToken(userId: String) =
        userActivationTokensRepository.save(
            UserActivationToken(
                userId = userId,
                token = RandomStringUtils.secure().nextAlphanumeric(100),
                expiresAt = timeService.currentTime().plus(
                    userManagementProperties.activation.tokenTtlInHours.toLong(),
                    ChronoUnit.HOURS,
                )
            )
        )

    fun activateUser(token: String, password: String) {
        val userActivationToken = userActivationTokensRepository.findByToken(token)
            ?: throw EntityNotFoundException("User activation token not found $token")

        if (userActivationToken.expired) {
            userActivationTokensRepository.delete(userActivationToken)
            throw UserActivationException.TokenExpiredException()
        }

        val user = userRepository.findById(userActivationToken.userId)
            // should never happen due to DB constraints
            .orElseThrow { IllegalStateException("User ${userActivationToken.userId} is not found") }

        val activatedUser = authenticationService.getObject()
            .setUserPassword(user, password)
            .copy(activated = true)

        userRepository.save(activatedUser)
        userActivationTokensRepository.delete(userActivationToken)
    }

    private val UserActivationToken.expired: Boolean
        get() = expiresAt.isBefore(timeService.currentTime())
}
