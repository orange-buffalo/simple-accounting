package io.orangebuffalo.simpleaccounting.infra

import org.springframework.stereotype.Component
import java.util.*

private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

@Component
class TokenGenerator {

    fun generateToken(tokenLength: Int = 64) = (1..tokenLength)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")

    fun generateUuid() = UUID.randomUUID().toString()
}
