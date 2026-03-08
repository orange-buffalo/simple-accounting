package io.orangebuffalo.simpleaccounting.infra

import io.kotest.matchers.string.shouldHaveLength
import org.junit.jupiter.api.Test

internal class TokenGeneratorTest {

    @Test
    fun `should generate a string of proper length`() {
        val tokenGenerator = TokenGenerator()
        tokenGenerator.generateToken(5).shouldHaveLength(5)
    }
}
