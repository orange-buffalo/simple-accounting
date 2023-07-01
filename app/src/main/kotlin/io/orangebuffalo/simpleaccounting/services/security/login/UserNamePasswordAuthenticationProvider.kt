package io.orangebuffalo.simpleaccounting.services.security.login

import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.security.mono
import io.orangebuffalo.simpleaccounting.services.security.toSecurityPrincipal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.withTimeout
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Duration

private const val MAX_FAILED_ATTEMPTS_BEFORE_LOCKING: Int = 5
private const val INITIAL_LOCK_PERIOD_MS: Long = 60000
private val MAX_LOCK_PERIOD_MS: BigDecimal = Duration.ofHours(24).toMillis().toBigDecimal()
private val LOCKING_TIME_PROGRESSION_RATIO: BigDecimal = BigDecimal.valueOf(1.5)
private const val AUTHENTICATION_REQUEST_TIMEOUT_MS: Long = 3000

@Component
class UserNamePasswordAuthenticationProvider(
    private val platformUserService: PlatformUserService,
    private val passwordEncoder: PasswordEncoder,
    private val timeService: TimeService
) : ReactiveAuthenticationManager {

    @Suppress("EXPERIMENTAL_API_USAGE")
    private val authenticationRequestsOrchestrator = GlobalScope.actor<AuthenticationRequest> {
        // throttles requests per user
        val perUserRequestsProcessor = mutableMapOf<String, SendChannel<AuthenticationRequest>>()

        for (authenticationRequest in channel) {
            val userName = authenticationRequest.authentication.name
            val userRequestProcessor = perUserRequestsProcessor.computeIfAbsent(userName) {
                actor(capacity = Channel.CONFLATED) {
                    for (userAuthenticationRequest in channel) {
                        try {
                            val authenticatedToken = doAuthenticate(userAuthenticationRequest.authentication)
                            userAuthenticationRequest.response.complete(authenticatedToken)
                        } catch (e: AuthenticationException) {
                            userAuthenticationRequest.response.completeExceptionally(e)
                        }
                    }
                }
            }
            // fire and forget the request - throttler will either process the request if it is available,
            // or skips it if there is an in-flight request already
            userRequestProcessor.trySend(authenticationRequest)
        }
    }

    private suspend fun doAuthenticate(usernamePasswordToken: UsernamePasswordAuthenticationToken): Authentication {
        val user = platformUserService.getUserByUserName(usernamePasswordToken.name)
            ?: throw BadCredentialsException("Invalid Credentials")
        return try {
            validateTemporaryLock(user)
            validatePassword(usernamePasswordToken, user)
            resetLoginStatistics(user)
            convertToAuthenticationToken(user)
        } catch (badCredentialsException: BadCredentialsException) {
            updateLoginStatisticsOnBadCredentials(user)
            throw badCredentialsException
        }
    }

    override fun authenticate(authentication: Authentication): Mono<Authentication> = authentication
        .mono<UsernamePasswordAuthenticationToken> { usernamePasswordToken ->
            val authenticationResponse = CompletableDeferred<Authentication>()
            authenticationRequestsOrchestrator.send(
                AuthenticationRequest(
                    authentication = usernamePasswordToken,
                    response = authenticationResponse
                )
            )

            try {
                // as throttler might skip our request, set the guard to not wait forever
                withTimeout(AUTHENTICATION_REQUEST_TIMEOUT_MS) {
                    authenticationResponse.await()
                }
            } catch (e: TimeoutCancellationException) {
                throw LoginUnavailableException()
            }
        }

    private suspend fun updateLoginStatisticsOnBadCredentials(user: PlatformUser) {
        val loginStatistics = user.loginStatistics
        loginStatistics.failedAttemptsCount++
        if (loginStatistics.failedAttemptsCount > MAX_FAILED_ATTEMPTS_BEFORE_LOCKING) {
            val numberOfLockingAttempts = loginStatistics.failedAttemptsCount - MAX_FAILED_ATTEMPTS_BEFORE_LOCKING
            val lockPeriodInMs = INITIAL_LOCK_PERIOD_MS.toBigDecimal()
                .multiply(LOCKING_TIME_PROGRESSION_RATIO.pow(numberOfLockingAttempts - 1))
                .min(MAX_LOCK_PERIOD_MS)
                .toLong()
            loginStatistics.temporaryLockExpirationTime = timeService.currentTime().plusMillis(lockPeriodInMs)
            platformUserService.save(user)
            throw AccountIsTemporaryLockedException(lockPeriodInMs / 1000)
        }
        platformUserService.save(user)
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

    private fun convertToAuthenticationToken(user: PlatformUser): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken(
            user.toSecurityPrincipal(),
            user.passwordHash
        )

    private suspend fun resetLoginStatistics(user: PlatformUser) {
        user.loginStatistics.reset()
        platformUserService.save(user)
    }

    private fun validatePassword(
        usernamePasswordToken: UsernamePasswordAuthenticationToken,
        user: PlatformUser
    ) {
        if (!passwordEncoder.matches(usernamePasswordToken.credentials as String, user.passwordHash)) {
            throw BadCredentialsException("Invalid Credentials")
        }
    }

    private data class AuthenticationRequest(
        val authentication: UsernamePasswordAuthenticationToken,
        val response: CompletableDeferred<Authentication>
    )
}
