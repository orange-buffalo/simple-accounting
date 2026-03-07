package io.orangebuffalo.simpleaccounting.business.security.jwt

import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshToken
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensRepository
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensService
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.coroutines.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.Instant
import java.time.temporal.ChronoUnit

class RefreshTokensServiceTest(
    @Autowired private val refreshTokensService: RefreshTokensService,
    @Autowired private val refreshTokensRepository: RefreshTokensRepository,
) : SaIntegrationTestBase() {

    @Test
    fun `should generate a new refresh token`() {
        val currentTime = Instant.parse("2018-05-03T16:03:23Z")
        val expirationTime = Instant.parse("2018-06-02T16:03:23Z")

        whenever(timeService.currentTime()) doReturn currentTime

        val token = runBlocking {
            refreshTokensService.generateRefreshToken(preconditions.fry.userName)
        }

        token.shouldNotBeNull().shouldStartWith("${preconditions.fry.id}:")

        val refreshToken = refreshTokensRepository.findByToken(token)
        refreshToken.shouldNotBeNull().also {
            it.userId.shouldBe(preconditions.fry.id)
            it.expirationTime.shouldBe(expirationTime)
        }
    }

    @Test
    fun `should build user details if token is valid`() {
        whenever(timeService.currentTime()) doReturn MOCK_TIME

        val userDetails = runBlocking {
            refreshTokensService.validateTokenAndBuildUserDetails(preconditions.refreshToken.token)
        }

        userDetails.shouldNotBeNull().also {
            it.username.shouldBe("Fry")
            it.authorities.shouldContain(SimpleGrantedAuthority("ROLE_USER"))
        }
    }

    @Test
    fun `should fail on validation if token is expired`() {
        whenever(timeService.currentTime()) doReturn MOCK_TIME.plus(30, ChronoUnit.DAYS).plusMillis(1)

        shouldThrow<BadCredentialsException> {
            runBlocking { refreshTokensService.validateTokenAndBuildUserDetails(preconditions.refreshToken.token) }
        }.message.shouldBe("Token expired")
    }

    @Test
    fun `should fail on validation if token is not found`() {
        shouldThrow<BadCredentialsException> { runBlocking { refreshTokensService.validateTokenAndBuildUserDetails("??") } }
            .message.shouldBe("Bad token")
    }

    @Test
    fun `should prolong the token`() {
        whenever(timeService.currentTime()) doReturn MOCK_TIME.minus(100, ChronoUnit.DAYS)

        val updatedTokenString = runBlocking {
            refreshTokensService.prolongToken(preconditions.refreshToken.token)
        }

        updatedTokenString.shouldBe(preconditions.refreshToken.token)

        val updatedToken = refreshTokensRepository.findByToken(updatedTokenString)!!
        updatedToken.expirationTime.shouldBe(MOCK_TIME.minus(70, ChronoUnit.DAYS))
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val refreshToken = RefreshToken(
                userId = fry.id!!,
                token = "42:34jFbT3h2=",
                expirationTime = MOCK_TIME
            ).save()
        }
    }
}
