package io.orangebuffalo.simpleaccounting.services.security.remeberme

import io.orangebuffalo.simpleaccounting.domain.users.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.RefreshToken
import io.orangebuffalo.simpleaccounting.services.persistence.repos.RefreshTokenRepository
import io.orangebuffalo.simpleaccounting.services.security.toSecurityPrincipal
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.temporal.ChronoUnit
import java.util.*

const val TOKEN_LIFETIME_IN_DAYS = 30L
private const val TOKEN_LENGTH = 1024

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userService: PlatformUserService,
    private val timeService: TimeService
) {
    private val random = SecureRandom()

    suspend fun generateRefreshToken(userName: String): String {
        val user = userService.getUserByUserName(userName)
            ?: throw IllegalArgumentException("$userName is not found")

        val tokenBytes = ByteArray(TOKEN_LENGTH)
        random.nextBytes(tokenBytes)
        val tokenString = "${user.id}:${String(Base64.getEncoder().encode(tokenBytes))}"

        val token = RefreshToken(
            user.id!!,
            tokenString,
            timeService.currentTime().plus(TOKEN_LIFETIME_IN_DAYS, ChronoUnit.DAYS)
        )

        withDbContext {
            refreshTokenRepository.save(token)
        }

        return tokenString
    }

    suspend fun validateTokenAndBuildUserDetails(refreshTokenString: String): UserDetails {
        val token = withDbContext {
            refreshTokenRepository.findByToken(refreshTokenString)
                ?: throw BadCredentialsException("Bad token")
        }

        if (timeService.currentTime().isAfter(token.expirationTime)) {
            throw BadCredentialsException("Token expired")
        }

        val tokenOwner = userService.getUserByUserId(token.userId)

        return tokenOwner.toSecurityPrincipal()
    }

    suspend fun prolongToken(refreshTokenString: String): String =
        withDbContext {
            val refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                ?: throw IllegalArgumentException("Bad token $refreshTokenString")
            refreshToken.expirationTime = timeService.currentTime().plus(TOKEN_LIFETIME_IN_DAYS, ChronoUnit.DAYS)
            refreshTokenRepository.save(refreshToken)
            refreshToken.token
        }
}
