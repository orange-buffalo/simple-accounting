package io.orangebuffalo.simpleaccounting.business.security.jwt

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.security.SaUserRoles
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.createRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.createTransientUserPrincipal
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeJsonInteger
import net.javacrumbs.jsonunit.kotest.inPath
import net.javacrumbs.jsonunit.kotest.shouldBeJsonArray
import net.javacrumbs.jsonunit.kotest.shouldBeJsonBoolean
import net.javacrumbs.jsonunit.kotest.shouldBeJsonString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.BadCredentialsException
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

private const val BAD_TOKEN =
    "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJGcnkiLCJhdXRob3JpdGllcyI6WyJST0xFX0RFTElWRVJZX0JPWSJdfQ.invalid"

private val VALID_TILL: Instant = ZonedDateTime.now().plusDays(2).toInstant()

@ExtendWith(MockitoExtension::class)
class JwtServiceTest {

    @Mock
    lateinit var timeService: TimeService

    @InjectMocks
    lateinit var jwtService: JwtService

    @Test
    fun `Should build a JWT token with proper claims for regular user`() {
        whenever(timeService.currentTime()) doReturn VALID_TILL

        val securityPrincipal = createRegularUserPrincipal("Fry", "", listOf("DELIVERY_BOY"))

        val actualToken = jwtService.buildJwtToken(securityPrincipal)

        assertThat(actualToken).matches(""".+\..+\..+""")

        val jwtBody = String(Base64.getUrlDecoder().decode(actualToken.split(".")[1]))

        jwtBody.inPath("sub").shouldBeJsonString().shouldBe("Fry")

        jwtBody.inPath("roles").shouldBeJsonArray().shouldContainExactlyInAnyOrder("DELIVERY_BOY")

        jwtBody.inPath("transient").shouldBeJsonBoolean().shouldBeFalse()

        jwtBody.inPath("exp").shouldBeJsonInteger().shouldBe(VALID_TILL.epochSecond + 600)
    }

    @Test
    fun `Should build a JWT token with proper claims for transient user`() {
        whenever(timeService.currentTime()) doReturn VALID_TILL

        val securityPrincipal = createTransientUserPrincipal("workspaceAccessToken", "")

        val actualToken = jwtService.buildJwtToken(securityPrincipal)
        assertThat(actualToken).matches(".+\\..+\\..+")

        val jwtBody = String(Base64.getUrlDecoder().decode(actualToken.split(".")[1]))

        jwtBody.inPath("sub").shouldBeJsonString().shouldBe("workspaceAccessToken")

        jwtBody.inPath("roles").shouldBeJsonArray().shouldContainExactlyInAnyOrder(SaUserRoles.USER)

        jwtBody.inPath("transient").shouldBeJsonBoolean().shouldBeTrue()

        jwtBody.inPath("exp").shouldBeJsonInteger().shouldBe(VALID_TILL.epochSecond + 600)
    }

    @Test
    fun `Should build a JWT token with custom expiration`() {
        val securityPrincipal = createRegularUserPrincipal("Fry", "", listOf("DELIVERY_BOY"))

        val actualToken = jwtService.buildJwtToken(securityPrincipal, VALID_TILL)

        assertThat(actualToken).matches(""".+\..+\..+""")

        val jwtBody = String(Base64.getUrlDecoder().decode(actualToken.split(".")[1]))

        jwtBody.inPath("sub").shouldBeJsonString().shouldBe("Fry")

        jwtBody.inPath("roles").shouldBeJsonArray().shouldContainExactlyInAnyOrder("DELIVERY_BOY")

        jwtBody.inPath("transient").shouldBeJsonBoolean().shouldBeFalse()

        jwtBody.inPath("exp").shouldBeJsonInteger().shouldBe(VALID_TILL.epochSecond)
    }

    @Test
    fun `Should parse the previously built token for regular user`() {
        whenever(timeService.currentTime()) doReturn VALID_TILL

        val inputPrincipal = createRegularUserPrincipal("Fry", "", listOf("DELIVERY_BOY"))
        val token = jwtService.buildJwtToken(inputPrincipal)

        val userDetails = jwtService.validateTokenAndBuildUserDetails(token)

        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo("Fry")
        assertThat(userDetails.authorities.map { it.authority }).containsExactlyInAnyOrder("ROLE_DELIVERY_BOY")
        assertThat(userDetails.password).isEqualTo(token)
        assertThat(userDetails.isAccountNonExpired).isTrue()
        assertThat(userDetails.isAccountNonLocked).isTrue()
        assertThat(userDetails.isCredentialsNonExpired).isTrue()
        assertThat(userDetails.isEnabled).isTrue()
        assertThat(userDetails).isInstanceOfSatisfying(SecurityPrincipal::class.java) {
            assertThat(it.isTransient).isFalse()
        }
    }

    @Test
    fun `Should parse the previously built token for transient user`() {
        whenever(timeService.currentTime()) doReturn VALID_TILL

        val inputPrincipal = createTransientUserPrincipal("workspaceAccessToken", "")
        val token = jwtService.buildJwtToken(inputPrincipal)

        val userDetails = jwtService.validateTokenAndBuildUserDetails(token)

        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo("workspaceAccessToken")
        assertThat(userDetails.authorities.map { it.authority }).containsExactlyInAnyOrder("ROLE_USER")
        assertThat(userDetails.password).isEqualTo(token)
        assertThat(userDetails.isAccountNonExpired).isTrue()
        assertThat(userDetails.isAccountNonLocked).isTrue()
        assertThat(userDetails.isCredentialsNonExpired).isTrue()
        assertThat(userDetails.isEnabled).isTrue()
        assertThat(userDetails).isInstanceOfSatisfying(SecurityPrincipal::class.java) {
            assertThat(it.isTransient).isTrue()
        }
    }

    @Test
    fun `Should fail on invalid token`() {
        val actualException = assertThrows<BadCredentialsException> {
            jwtService.validateTokenAndBuildUserDetails(BAD_TOKEN)
        }
        assertThat(actualException.message).isEqualTo("Bad token $BAD_TOKEN")
    }
}
