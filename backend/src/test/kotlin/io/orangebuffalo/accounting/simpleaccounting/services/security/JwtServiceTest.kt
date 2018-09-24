package io.orangebuffalo.accounting.simpleaccounting.services.security

import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.User
import java.util.*

const val BAD_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJGcnkiLCJhdXRob3JpdGllcyI6WyJST0xFX0RFTElWRVJZX0JPWSJdfQ.invalid"

class JwtServiceTest {

    @Test
    fun `Should build a JWT token with proper claims`() {
        val jwtService = JwtService()

        val user = User.builder()
                .username("Fry")
                .roles("DELIVERY_BOY")
                .password("")
                .build()

        val actualToken = jwtService.buildJwtToken(user)
        assertThat(actualToken).matches(".+\\..+\\..+")

        val jwtBody = String(Base64.getUrlDecoder().decode(actualToken.split(".")[1]))

        assertThatJson(jwtBody)
                .node("sub").isString.isEqualTo("Fry")

        assertThatJson(jwtBody)
                .node("authorities").isArray.containsExactlyInAnyOrder("ROLE_DELIVERY_BOY")
    }

    @Test
    fun `Should parse the previously built token`() {
        val jwtService = JwtService()

        val user = User.builder()
                .username("Fry")
                .roles("DELIVERY_BOY")
                .password("")
                .build()
        val token = jwtService.buildJwtToken(user)

        val userDetails = jwtService.validateTokenAndBuildUserDetails(token)

        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo("Fry")
        assertThat(userDetails.authorities.map { it.authority }).containsExactlyInAnyOrder("ROLE_DELIVERY_BOY")
        assertThat(userDetails.password).isEqualTo(token)
        assertThat(userDetails.isAccountNonExpired).isTrue()
        assertThat(userDetails.isAccountNonLocked).isTrue()
        assertThat(userDetails.isCredentialsNonExpired).isTrue()
        assertThat(userDetails.isEnabled).isTrue()
    }

    @Test
    fun `Should fail on invalid token`() {
        val jwtService = JwtService()

        val actualException = assertThrows<BadTokenException> {
            jwtService.validateTokenAndBuildUserDetails(BAD_TOKEN)
        }
        assertThat(actualException.message).isEqualTo("Bad token $BAD_TOKEN")
    }
}