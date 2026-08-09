package io.orangebuffalo.simpleaccounting.business.security.remeberme

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersRepository
import io.orangebuffalo.simpleaccounting.infra.TimeService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.temporal.ChronoUnit
import java.util.*

const val TOKEN_LIFETIME_IN_DAYS = 30L
private const val TOKEN_LENGTH = 1024

@Service
class RefreshTokensService(
    private val refreshTokensRepository: RefreshTokensRepository,
    private val platformUsersRepository: PlatformUsersRepository,
    private val timeService: TimeService
) {
    private val random = SecureRandom()

    fun generateRefreshToken(userName: String): String {
        val user = platformUsersRepository.findByUserName(userName)
            ?: throw IllegalArgumentException("$userName is not found")

        val tokenBytes = ByteArray(TOKEN_LENGTH)
        random.nextBytes(tokenBytes)
        val tokenString = "${user.id}:${String(Base64.getEncoder().encode(tokenBytes))}"

        val token = RefreshToken(
            user.id!!,
            tokenString,
            timeService.currentTime().plus(TOKEN_LIFETIME_IN_DAYS, ChronoUnit.DAYS)
        )

        refreshTokensRepository.save(token)

        return tokenString
    }

    fun validateTokenAndBuildUserDetails(refreshTokenString: String): UserDetails {
        val token = refreshTokensRepository.findByToken(refreshTokenString)
            ?: throw BadCredentialsException("Bad token")

        if (timeService.currentTime().isAfter(token.expirationTime)) {
            throw BadCredentialsException("Token expired")
        }

        val tokenOwner = platformUsersRepository.findById(token.userId)
            .orElseThrow { EntityNotFoundException("User ${token.userId} is not found") }

        return tokenOwner.toSecurityPrincipal()
    }

    fun prolongToken(refreshTokenString: String): String {
        val refreshToken = refreshTokensRepository.findByToken(refreshTokenString)
            ?: throw IllegalArgumentException("Bad token $refreshTokenString")
        refreshTokensRepository.save(
            refreshToken.copy(
                expirationTime = timeService.currentTime().plus(TOKEN_LIFETIME_IN_DAYS, ChronoUnit.DAYS)
            )
        )
        return refreshToken.token
    }
}
