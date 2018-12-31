package io.orangebuffalo.accounting.simpleaccounting.services.security.jwt

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.RefreshToken
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.RefreshTokenRepository
import io.orangebuffalo.accounting.simpleaccounting.services.security.toUserDetails
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
            user,
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

        return token.user.toUserDetails()
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
