package io.orangebuffalo.accounting.simpleaccounting.services.integration

import assertk.assertThat
import assertk.assertions.hasLength
import org.junit.jupiter.api.Test

internal class TokenGeneratorTest {

    @Test
    fun `should generate a string of proper length`() {
        val tokenGenerator = TokenGenerator()
        assertThat(tokenGenerator.generateToken(5)).hasLength(5)
    }
}