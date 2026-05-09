package io.orangebuffalo.simpleaccounting.business.security.authentication

import io.orangebuffalo.simpleaccounting.business.users.LoginStatistics
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.security.getCurrentPrincipalOrNull
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration

private const val MAX_FAILED_ATTEMPTS_BEFORE_LOCKING: Int = 5
private const val INITIAL_LOCK_PERIOD_MS: Long = 60000
private val MAX_LOCK_PERIOD_MS: BigDecimal = Duration.ofHours(24).toMillis().toBigDecimal()
private val LOCKING_TIME_PROGRESSION_RATIO: BigDecimal = BigDecimal.valueOf(1.5)

@Service
class AuthenticationService(
    private val platformUsersService: PlatformUsersService,
    private val passwordEncoder: PasswordEncoder,
    private val timeService: TimeService,
) {

    suspend fun authenticate(userName: String, credentials: String): PlatformUser {
        val user = platformUsersService.getUserByUserName(userName)
            ?: throw BadCredentialsException("Invalid Credentials")
        validateActivated(user)
        validateTemporaryLock(user)
        validatePassword(user, credentials)
        resetLoginStatistics(user)
        return user
    }

    private fun validateActivated(user: PlatformUser) {
        if (!user.activated) {
            throw UserNotActivatedException()
        }
    }

    private fun validateTemporaryLock(user: PlatformUser) {
        val temporaryLockExpirationTime = user.loginStatistics.temporaryLockExpirationTime
        if (temporaryLockExpirationTime != null) {
            val currentTime = timeService.currentTime()
            if (!currentTime.isAfter(temporaryLockExpirationTime)) {
                throw AccountIsTemporaryLockedException(
                    Duration.between(currentTime, temporaryLockExpirationTime).seconds
                )
            }
        }
    }

    private suspend fun resetLoginStatistics(user: PlatformUser) {
        platformUsersService.save(
            user.copy(loginStatistics = LoginStatistics(failedAttemptsCount = 0, temporaryLockExpirationTime = null))
        )
    }

    private suspend fun validatePassword(
        user: PlatformUser,
        credentials: String,
    ) {
        if (!checkCredentials(user, credentials)) {
            updateLoginStatisticsOnBadCredentials(user)
            throw BadCredentialsException("Invalid Credentials")
        }
    }

    private fun checkCredentials(user: PlatformUser, credentials: String) =
        passwordEncoder.matches(credentials, user.passwordHash)

    private suspend fun updateLoginStatisticsOnBadCredentials(user: PlatformUser) {
        val failedAttemptsCount = user.loginStatistics.failedAttemptsCount + 1
        if (failedAttemptsCount > MAX_FAILED_ATTEMPTS_BEFORE_LOCKING) {
            val numberOfLockingAttempts = failedAttemptsCount - MAX_FAILED_ATTEMPTS_BEFORE_LOCKING
            val lockPeriodInMs = INITIAL_LOCK_PERIOD_MS.toBigDecimal()
                .multiply(LOCKING_TIME_PROGRESSION_RATIO.pow(numberOfLockingAttempts - 1))
                .min(MAX_LOCK_PERIOD_MS)
                .toLong()
            platformUsersService.save(
                user.copy(
                    loginStatistics = user.loginStatistics.copy(
                        failedAttemptsCount = failedAttemptsCount,
                        temporaryLockExpirationTime = timeService.currentTime().plusMillis(lockPeriodInMs),
                    )
                )
            )
            throw AccountIsTemporaryLockedException(lockPeriodInMs / 1000)
        }
        platformUsersService.save(
            user.copy(loginStatistics = user.loginStatistics.copy(failedAttemptsCount = failedAttemptsCount))
        )
    }

    suspend fun changeCurrentUserPassword(currentPassword: String, newPassword: String) {
        val currentPrincipal = getCurrentPrincipalOrNull()
            ?: throw PasswordChangeException.UserNotAuthenticatedException()
        if (currentPrincipal.isTransient) {
            throw PasswordChangeException.TransientUserException()
        }
        val user = platformUsersService.getUserByUserName(currentPrincipal.userName)
            ?: throw IllegalStateException("Current principal is not resolved to a user")
        if (!checkCredentials(user, currentPassword)) {
            throw PasswordChangeException.InvalidCurrentPasswordException()
        }
        platformUsersService.save(setUserPassword(user, newPassword))
    }

    fun setUserPassword(user: PlatformUser, password: String): PlatformUser =
        user.copy(passwordHash = passwordEncoder.encode(password))
}

sealed class PasswordChangeException(message: String) : RuntimeException(message) {
    class TransientUserException : PasswordChangeException("Cannot change password for transient user")
    class InvalidCurrentPasswordException : PasswordChangeException("Invalid current password")
    class UserNotAuthenticatedException : PasswordChangeException("User is not authenticated")
}

class UserNotActivatedException : AuthenticationException("User is not activated")
