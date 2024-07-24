package io.orangebuffalo.simpleaccounting.business.security.jwt

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.createRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.createTransientUserPrincipal
import io.orangebuffalo.simpleaccounting.infra.TimeService
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
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

private val VALID_TILL : Instant = ZonedDateTime.now().plusDays(2).toInstant()

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

        assertThatJson(jwtBody)
            .node("sub").isString.isEqualTo("Fry")

        assertThatJson(jwtBody)
            .node("roles").isArray.containsExactlyInAnyOrder("DELIVERY_BOY")

        assertThatJson(jwtBody)
            .node("transient").isBoolean.isEqualTo(false)

        assertThatJson(jwtBody)
            .node("exp").isNumber.isEqualTo((VALID_TILL.epochSecond + 600).toBigDecimal())
    }

    @Test
    fun `Should build a JWT token with proper claims for transient user`() {
        whenever(timeService.currentTime()) doReturn VALID_TILL

        val securityPrincipal = createTransientUserPrincipal("workspaceAccessToken", "")

        val actualToken = jwtService.buildJwtToken(securityPrincipal)
        assertThat(actualToken).matches(".+\\..+\\..+")

        val jwtBody = String(Base64.getUrlDecoder().decode(actualToken.split(".")[1]))

        assertThatJson(jwtBody)
            .node("sub").isString.isEqualTo("workspaceAccessToken")

        assertThatJson(jwtBody)
            .node("roles").isArray.containsExactlyInAnyOrder("USER")

        assertThatJson(jwtBody)
            .node("transient").isBoolean.isEqualTo(true)

        assertThatJson(jwtBody)
            .node("exp").isNumber.isEqualTo((VALID_TILL.epochSecond + 600).toBigDecimal())
    }

    @Test
    fun `Should build a JWT token with custom expiration`() {
        val securityPrincipal = createRegularUserPrincipal("Fry", "", listOf("DELIVERY_BOY"))

        val actualToken = jwtService.buildJwtToken(securityPrincipal, VALID_TILL)

        assertThat(actualToken).matches(""".+\..+\..+""")

        val jwtBody = String(Base64.getUrlDecoder().decode(actualToken.split(".")[1]))

        assertThatJson(jwtBody)
            .node("sub").isString.isEqualTo("Fry")

        assertThatJson(jwtBody)
            .node("roles").isArray.containsExactlyInAnyOrder("DELIVERY_BOY")

        assertThatJson(jwtBody)
            .node("transient").isBoolean.isEqualTo(false)

        assertThatJson(jwtBody)
            .node("exp").isNumber.isEqualTo((VALID_TILL.epochSecond).toBigDecimal())
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
