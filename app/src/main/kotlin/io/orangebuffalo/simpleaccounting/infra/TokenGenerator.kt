package io.orangebuffalo.simpleaccounting.infra

import io.orangebuffalo.simpleaccounting.infra.thirdparty.JbrWorkaround
import org.springframework.stereotype.Component

private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

interface TokenGenerator {

    fun generateToken(tokenLength: Int = 64) = (1..tokenLength)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

@JbrWorkaround
@Component
class TokenGeneratorImpl : TokenGenerator
