package io.orangebuffalo.simpleaccounting.services.security.jwt

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.RefreshToken
import io.orangebuffalo.simpleaccounting.services.persistence.repos.RefreshTokenRepository
import io.orangebuffalo.simpleaccounting.services.security.remeberme.RefreshTokenService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Consumer

@SimpleAccountingIntegrationTest
class RefreshTokenServiceIT(
    @Autowired private val refreshTokenService: RefreshTokenService,
    @Autowired private val refreshTokenRepository: RefreshTokenRepository,
    @Autowired private val timeService: TimeService,
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @Test
    fun `should generate a new refresh token`() {
        val testData = setupPreconditions()

        val currentTime = Instant.parse("2018-05-03T16:03:23Z")
        val expirationTime = Instant.parse("2018-06-02T16:03:23Z")

        whenever(timeService.currentTime()) doReturn currentTime

        val token = runBlocking {
            refreshTokenService.generateRefreshToken(testData.fry.userName)
        }

        assertThat(token).isNotNull().startsWith("${testData.fry.id}:")

        val refreshToken = refreshTokenRepository.findByToken(token)
        assertThat(refreshToken).isNotNull.satisfies(Consumer {
            assertThat(it!!.userId).isEqualTo(testData.fry.id)
            assertThat(it.expirationTime).isEqualTo(expirationTime)
        })
    }

    @Test
    fun `should build user details if token is valid`() {
        val testData = setupPreconditions()

        whenever(timeService.currentTime()) doReturn MOCK_TIME

        val userDetails = runBlocking {
            refreshTokenService.validateTokenAndBuildUserDetails(testData.refreshToken.token)
        }

        assertThat(userDetails).isNotNull.satisfies(Consumer {
            assertThat(it.username).isEqualTo("Fry")
            assertThat(it.authorities).contains(SimpleGrantedAuthority("ROLE_USER"))
        })
    }

    @Test
    fun `should fail on validation if token is expired`() {
        val testData = setupPreconditions()

        whenever(timeService.currentTime()) doReturn MOCK_TIME.plus(30, ChronoUnit.DAYS).plusMillis(1)

        assertThatThrownBy {
            runBlocking { refreshTokenService.validateTokenAndBuildUserDetails(testData.refreshToken.token) }
        }
            .isInstanceOf(BadCredentialsException::class.java)
            .hasMessage("Token expired")
    }

    @Test
    fun `should fail on validation if token is not found`() {
        setupPreconditions()

        assertThatThrownBy { runBlocking { refreshTokenService.validateTokenAndBuildUserDetails("??") } }
            .isInstanceOf(BadCredentialsException::class.java)
            .hasMessage("Bad token")
    }

    @Test
    fun `should prolong the token`() {
        val testData = setupPreconditions()

        whenever(timeService.currentTime()) doReturn MOCK_TIME.minus(100, ChronoUnit.DAYS)

        val updatedTokenString = runBlocking {
            refreshTokenService.prolongToken(testData.refreshToken.token)
        }

        assertThat(updatedTokenString).isEqualTo(testData.refreshToken.token)

        val updatedToken = refreshTokenRepository.findByToken(updatedTokenString)!!
        assertThat(updatedToken.expirationTime).isEqualTo(MOCK_TIME.minus(70, ChronoUnit.DAYS))
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
        val fry = fry()
        val refreshToken = RefreshToken(
            userId = fry.id!!,
            token = "42:34jFbT3h2=",
            expirationTime = MOCK_TIME
        ).save()
    }
}
