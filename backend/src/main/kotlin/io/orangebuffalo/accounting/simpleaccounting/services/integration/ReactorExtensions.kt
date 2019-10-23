package io.orangebuffalo.accounting.simpleaccounting.services.integration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import reactor.core.publisher.Mono
import kotlin.coroutines.coroutineContext

// todo #87: probably we need another name, maybe buildMono / createMono / coroutineMono
fun <T> toMono(block: suspend CoroutineScope.() -> T): Mono<T> = ReactiveSecurityContextHolder.getContext()
    .map { it.authentication }
    .flatMap { authentication ->
        mono(CoroutineAuthentication(authentication)) {
            block()
        }
    }

suspend inline fun <T> Mono<T>.awaitMonoOrNull(): T? {
    val authentication = coroutineContext.getAuthenticationOrNull()
    val mono = if (authentication == null) {
        this
    } else {
        this.subscriberContext(ReactiveSecurityContextHolder.withAuthentication(authentication))
    }
    return mono.awaitFirstOrNull()
}

//todo #87: maybe give another though about the name
suspend inline fun <T> Mono<T>.awaitMono(): T {
    return awaitMonoOrNull()
        ?: throw IllegalStateException("Mono did not return a value")
}