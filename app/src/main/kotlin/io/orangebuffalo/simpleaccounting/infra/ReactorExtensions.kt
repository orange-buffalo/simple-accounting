package io.orangebuffalo.simpleaccounting.infra

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

fun voidMono(block: suspend CoroutineScope.() -> Unit): Mono<Void> = mono {
    block()
    null
}
