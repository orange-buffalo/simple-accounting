package io.orangebuffalo.simpleaccounting.business.security.authentication

import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

private const val AUTHENTICATION_REQUEST_TIMEOUT_MS: Long = 3000

@Component
class UserNamePasswordAuthenticationProvider(
    private val authenticationService: AuthenticationService,
) : AuthenticationProvider {

    private val authenticationLocks = ConcurrentHashMap<String, ReentrantLock>()

    override fun authenticate(authentication: Authentication): Authentication? {
        if (authentication !is UsernamePasswordAuthenticationToken) return null
        val lock = authenticationLocks.computeIfAbsent(authentication.name) { ReentrantLock() }
        val acquired = try {
            lock.tryLock(AUTHENTICATION_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!acquired) throw LoginUnavailableException()
        return try {
            val authenticatedUser = authenticationService.authenticate(
                userName = authentication.name,
                credentials = authentication.credentials as String
            )
            convertToAuthenticationToken(authenticatedUser)
        } finally {
            lock.unlock()
        }
    }

    override fun supports(authentication: Class<*>): Boolean =
        UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)

    private fun convertToAuthenticationToken(user: PlatformUser): UsernamePasswordAuthenticationToken {
        val principal = user.toSecurityPrincipal()
        return UsernamePasswordAuthenticationToken(
            principal,
            user.passwordHash,
            principal.authorities,
        )
    }
}
