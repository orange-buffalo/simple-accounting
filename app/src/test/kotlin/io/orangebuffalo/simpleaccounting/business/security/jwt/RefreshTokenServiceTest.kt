package io.orangebuffalo.simpleaccounting.business.security.jwt

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshToken
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokenRepository
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokenService
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
class RefreshTokenServiceTest(
    @Autowired private val refreshTokenService: RefreshTokenService,
    @Autowired private val refreshTokenRepository: RefreshTokenRepository,
    @Autowired private val timeService: TimeService,
    preconditionsFactory: PreconditionsFactory,
) {

    @Test
    fun `should generate a new refresh token`() {
        val currentTime = Instant.parse("2018-05-03T16:03:23Z")
        val expirationTime = Instant.parse("2018-06-02T16:03:23Z")

        whenever(timeService.currentTime()) doReturn currentTime

        val token = runBlocking {
            refreshTokenService.generateRefreshToken(preconditions.fry.userName)
        }

        assertThat(token).isNotNull().startsWith("${preconditions.fry.id}:")

        val refreshToken = refreshTokenRepository.findByToken(token)
        assertThat(refreshToken).isNotNull.satisfies(Consumer {
            assertThat(it!!.userId).isEqualTo(preconditions.fry.id)
            assertThat(it.expirationTime).isEqualTo(expirationTime)
        })
    }

    @Test
    fun `should build user details if token is valid`() {
        whenever(timeService.currentTime()) doReturn MOCK_TIME

        val userDetails = runBlocking {
            refreshTokenService.validateTokenAndBuildUserDetails(preconditions.refreshToken.token)
        }

        assertThat(userDetails).isNotNull.satisfies(Consumer {
            assertThat(it.username).isEqualTo("Fry")
            assertThat(it.authorities).contains(SimpleGrantedAuthority("ROLE_USER"))
        })
    }

    @Test
    fun `should fail on validation if token is expired`() {
        whenever(timeService.currentTime()) doReturn MOCK_TIME.plus(30, ChronoUnit.DAYS).plusMillis(1)

        assertThatThrownBy {
            runBlocking { refreshTokenService.validateTokenAndBuildUserDetails(preconditions.refreshToken.token) }
        }
            .isInstanceOf(BadCredentialsException::class.java)
            .hasMessage("Token expired")
    }

    @Test
    fun `should fail on validation if token is not found`() {
        assertThatThrownBy { runBlocking { refreshTokenService.validateTokenAndBuildUserDetails("??") } }
            .isInstanceOf(BadCredentialsException::class.java)
            .hasMessage("Bad token")
    }

    @Test
    fun `should prolong the token`() {
        whenever(timeService.currentTime()) doReturn MOCK_TIME.minus(100, ChronoUnit.DAYS)

        val updatedTokenString = runBlocking {
            refreshTokenService.prolongToken(preconditions.refreshToken.token)
        }

        assertThat(updatedTokenString).isEqualTo(preconditions.refreshToken.token)

        val updatedToken = refreshTokenRepository.findByToken(updatedTokenString)!!
        assertThat(updatedToken.expirationTime).isEqualTo(MOCK_TIME.minus(70, ChronoUnit.DAYS))
    }

    private val preconditions by preconditionsFactory {
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
