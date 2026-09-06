package io.orangebuffalo.simpleaccounting.infra

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.*

private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

@Component
class TokenGenerator {

    private val secureRandom = SecureRandom()

    /**
     * Generates a token for values that are not security sensitive, like entity IDs.
     * @see generateSecureToken for secrets
     */
    fun generateToken(tokenLength: Int = 64) = (1..tokenLength)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")

    /**
     * Generates a token suitable for secrets, i.e. values that must not be guessable
     * by an attacker who observed other generated values.
     */
    fun generateSecureToken(tokenLength: Int = 64) = (1..tokenLength)
        .map { secureRandom.nextInt(charPool.size) }
        .map(charPool::get)
        .joinToString("")

    fun generateUuid() = UUID.randomUUID().toString()
}
