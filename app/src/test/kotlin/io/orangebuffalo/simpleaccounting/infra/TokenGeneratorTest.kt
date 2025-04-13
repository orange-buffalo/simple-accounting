package io.orangebuffalo.simpleaccounting.infra

import assertk.assertThat
import assertk.assertions.hasLength
import org.junit.jupiter.api.Test

internal class TokenGeneratorTest {

    @Test
    fun `should generate a string of proper length`() {
        val tokenGenerator = TokenGeneratorImpl()
        assertThat(tokenGenerator.generateToken(5)).hasLength(5)
    }
}
