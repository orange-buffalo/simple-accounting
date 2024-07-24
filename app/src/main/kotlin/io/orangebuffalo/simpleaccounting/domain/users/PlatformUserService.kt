package io.orangebuffalo.simpleaccounting.domain.users

import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.domain.security.authentication.AuthenticationService
import io.orangebuffalo.simpleaccounting.domain.security.ensureRegularUserPrincipal
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.ObjectProvider
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

/**
 * Service for managing platform users, including their activation tokens.
 */
@Service
class PlatformUserService(
    private val userRepository: PlatformUserRepository,
    private val userActivationTokenRepository: UserActivationTokenRepository,
    private val userManagementProperties: UserManagementProperties,
    private val timeService: TimeService,
    // object provider is used to avoid circular dependencies
    private val authenticationService: ObjectProvider<AuthenticationService>,
) {

    suspend fun getCurrentUser(): PlatformUser = withDbContext {
        userRepository.findByUserName(ensureRegularUserPrincipal().userName)
            ?: throw IllegalStateException("Current principal is not resolved to a user")
    }

    suspend fun getUserByUserName(userName: String): PlatformUser? = withDbContext {
        userRepository.findByUserName(userName)
    }

    suspend fun save(user: PlatformUser): PlatformUser = withDbContext {
        userRepository.save(user)
    }

    /**
     * Validates and saves updated user data.
     * @throws UserUpdateException.UserAlreadyExistsException in case another user with the same name already exists
     */
    suspend fun updateUser(updatedUserData: PlatformUser): PlatformUser = withDbContext {
        check(updatedUserData.id != null) {
            "User id must be provided for update"
        }
        val existingUser = userRepository.findByUserName(updatedUserData.userName)
        if (existingUser != null && existingUser.id != updatedUserData.id) {
            throw UserUpdateException.UserAlreadyExistsException(updatedUserData.userName)
        }
        userRepository.save(updatedUserData)
    }

    /**
     * Creates a new user.
     * @throws UserCreationException.UserAlreadyExistsException in case another user with the same name already exists
     */
    suspend fun createUser(
        userName: String,
        isAdmin: Boolean,
    ): PlatformUser {
        withDbContext {
            val existingUser = userRepository.findByUserName(userName)
            if (existingUser != null) {
                throw UserCreationException.UserAlreadyExistsException(userName)
            }
        }

        val user = save(
            PlatformUser(
                userName = userName,
                passwordHash = RandomStringUtils.random(100),
                isAdmin = isAdmin,
                i18nSettings = I18nSettings(locale = "en_AU", language = "en"),
                activated = false
            )
        )
        setupUserActivationToken(user.id!!)
        return user
    }

    suspend fun getUserByUserId(userId: Long): PlatformUser = withDbContext {
        userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User $userId is not found") }
    }

    /**
     * Retrieves user activation token by user id. There is always at most one token per user.
     * In case the token is not found, or is expired, null is returned.
     * In case the token is expired, it is removed.
     */
    suspend fun getUserActivationTokenForUser(userId: Long): UserActivationToken? {
        val token = withDbContext {
            userActivationTokenRepository.findByUserId(userId)
        }
        return ensureTokenIsNotExpired(token)
    }

    private suspend fun ensureTokenIsNotExpired(token: UserActivationToken?): UserActivationToken? {
        if (token == null) return null
        if (token.expired) {
            withDbContext {
                userActivationTokenRepository.delete(token)
            }
            return null
        }
        return token
    }

    /**
     * Retrieves user activation token by token value.
     * In case the token is not found, or is expired, null is returned.
     * In case the token is expired, it is removed.
     */
    suspend fun getUserActivationToken(tokenValue: String): UserActivationToken? {
        val token = withDbContext {
            userActivationTokenRepository.findByToken(tokenValue)
        }
        return ensureTokenIsNotExpired(token)
    }

    /**
     * Creates a new user activation token for the user with the specified id.
     * In case there is an existing token for the user, it is replaced with a new one.
     */
    suspend fun createUserActivationToken(userId: Long): UserActivationToken {
        val user = withDbContext {
            userRepository.findByIdOrNull(userId)
        }
        if (user == null) {
            throw EntityNotFoundException("User $userId is not found")
        }
        if (user.activated) {
            throw UserActivationTokenCreationException.UserAlreadyActivatedException(userId)
        }
        val token = withDbContext {
            userActivationTokenRepository.findByUserId(userId)
        }
        if (token != null) {
            withDbContext {
                userActivationTokenRepository.delete(token)
            }
        }
        return setupUserActivationToken(userId)
    }

    private suspend fun setupUserActivationToken(userId: Long) = withDbContext {
        userActivationTokenRepository.save(
            UserActivationToken(
                userId = userId,
                token = RandomStringUtils.randomAlphanumeric(100),
                expiresAt = timeService.currentTime().plus(
                    userManagementProperties.activation.tokenTtlInHours.toLong(),
                    ChronoUnit.HOURS,
                )
            )
        )
    }

    suspend fun activateUser(token: String, password: String) {
        val userActivationToken = withDbContext {
            userActivationTokenRepository.findByToken(token)
                ?: throw EntityNotFoundException("User activation token not found $token")
        }

        if (userActivationToken.expired) {
            withDbContext {
                userActivationTokenRepository.delete(userActivationToken)
            }
            throw UserActivationException.TokenExpiredException()
        }

        val user = withDbContext {
            userRepository.findById(userActivationToken.userId)
                // should never happen due to DB constraints
                .orElseThrow { IllegalStateException("User ${userActivationToken.userId} is not found") }
        }

        authenticationService.getObject().setUserPassword(user, password)
        user.activated = true

        withDbContext {
            userRepository.save(user)
            userActivationTokenRepository.delete(userActivationToken)
        }
    }

    private val UserActivationToken.expired: Boolean
        get() = expiresAt.isBefore(timeService.currentTime())
}
