package io.orangebuffalo.accounting.simpleaccounting.services.integration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import reactor.core.publisher.Mono

fun <T> toMono(block: suspend CoroutineScope.() -> T): Mono<T> = ReactiveSecurityContextHolder.getContext()
    .map { it.authentication.principal }
    .cast(UserDetails::class.java)
    .flatMap { principal ->
        GlobalScope.mono(CoroutinePrincipal(principal)) {
            block()
        }
    }