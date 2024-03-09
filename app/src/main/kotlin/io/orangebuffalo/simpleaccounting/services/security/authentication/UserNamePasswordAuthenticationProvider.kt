package io.orangebuffalo.simpleaccounting.services.security.authentication

import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import io.orangebuffalo.simpleaccounting.services.security.mono
import io.orangebuffalo.simpleaccounting.services.security.toSecurityPrincipal
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

private const val AUTHENTICATION_REQUEST_TIMEOUT_MS: Long = 3000

@Component
class UserNamePasswordAuthenticationProvider(
    private val authenticationService: AuthenticationService,
) : ReactiveAuthenticationManager {

    @OptIn(ObsoleteCoroutinesApi::class, DelicateCoroutinesApi::class)
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
        val authenticatedUser = authenticationService.authenticate(
            userName = usernamePasswordToken.name,
            credentials = usernamePasswordToken.credentials as String
        )
        return convertToAuthenticationToken(authenticatedUser)
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

    private fun convertToAuthenticationToken(user: PlatformUser): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken(
            user.toSecurityPrincipal(),
            user.passwordHash
        )

    private data class AuthenticationRequest(
        val authentication: UsernamePasswordAuthenticationToken,
        val response: CompletableDeferred<Authentication>
    )
}
