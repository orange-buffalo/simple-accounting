package io.orangebuffalo.accounting.simpleaccounting.services.security.jwt

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Fry
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.RefreshTokenRepository
import io.orangebuffalo.accounting.simpleaccounting.web.MOCK_TIME
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RefreshTokenServiceTestIT(
    @Autowired private val refreshTokenService: RefreshTokenService,
    @Autowired private val refreshTokenRepository: RefreshTokenRepository
) {

    @MockBean
    lateinit var timeService: TimeService

    @Test
    fun `should generate a new refresh token`(fry: Fry) {
        val currentTime = Instant.parse("2018-05-03T16:03:23Z")
        val expirationTime = Instant.parse("2018-06-02T16:03:23Z")

        whenever(timeService.currentTime()) doReturn currentTime

        val token = runBlocking {
            refreshTokenService.generateRefreshToken(fry.himself.userName)
        }

        assertThat(token).isNotNull().startsWith("${fry.himself.id}:")

        val refreshToken = refreshTokenRepository.findByToken(token)
        assertThat(refreshToken).isNotNull.satisfies {
            assertThat(it!!.user).isEqualTo(fry.himself)
            assertThat(it.expirationTime).isEqualTo(expirationTime)
        }
    }

    @Test
    fun `should build user details if token is valid`(fry: Fry) {
        whenever(timeService.currentTime()) doReturn MOCK_TIME

        val userDetails = runBlocking {
            refreshTokenService.validateTokenAndBuildUserDetails(fry.refreshToken.token)
        }

        assertThat(userDetails).isNotNull.satisfies {
            assertThat(it.username).isEqualTo("Fry")
            assertThat(it.authorities).contains(SimpleGrantedAuthority("ROLE_USER"))
        }
    }

    @Test
    fun `should fail on validation if token is expired`(fry: Fry) {
        whenever(timeService.currentTime()) doReturn MOCK_TIME.plus(30, ChronoUnit.DAYS).plusMillis(1)

        assertThatThrownBy {
            runBlocking { refreshTokenService.validateTokenAndBuildUserDetails(fry.refreshToken.token) }
        }
            .isInstanceOf(BadCredentialsException::class.java)
            .hasMessage("Token expired")
    }

    @Test
    fun `should fail on validation if token is not found`(fry: Fry) {
        assertThatThrownBy { runBlocking { refreshTokenService.validateTokenAndBuildUserDetails("??") } }
            .isInstanceOf(BadCredentialsException::class.java)
            .hasMessage("Bad token")
    }

    @Test
    fun `should prolong the token`(fry: Fry) {
        whenever(timeService.currentTime()) doReturn MOCK_TIME.minus(100, ChronoUnit.DAYS)

        val updatedTokenString = runBlocking {
            refreshTokenService.prolongToken(fry.refreshToken.token)
        }

        assertThat(updatedTokenString).isEqualTo(fry.refreshToken.token)

        val updatedToken = refreshTokenRepository.findByToken(updatedTokenString)!!
        assertThat(updatedToken.expirationTime).isEqualTo(MOCK_TIME.minus(70, ChronoUnit.DAYS))
    }
}