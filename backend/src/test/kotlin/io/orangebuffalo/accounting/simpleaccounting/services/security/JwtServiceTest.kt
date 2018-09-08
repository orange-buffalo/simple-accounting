package io.orangebuffalo.accounting.simpleaccounting.services.security

import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.User
import java.util.*

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
}