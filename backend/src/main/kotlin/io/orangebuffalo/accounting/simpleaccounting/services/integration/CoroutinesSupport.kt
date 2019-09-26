package io.orangebuffalo.accounting.simpleaccounting.services.integration

import io.orangebuffalo.accounting.simpleaccounting.services.security.InsufficientUserType
import io.orangebuffalo.accounting.simpleaccounting.services.security.SecurityPrincipal
import kotlinx.coroutines.*
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.Authentication
import reactor.core.publisher.Mono
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

fun CoroutineScope.ensureRegularUserPrincipal(): SecurityPrincipal = getCurrentPrincipal()
    .apply { if (isTransient) throw InsufficientUserType() }

fun CoroutineScope.getCurrentPrincipal(): SecurityPrincipal {
    return coroutineContext.getPrincipal()
}

fun CoroutineContext.getPrincipal(): SecurityPrincipal {
    val authentication = this.getAuthentication()
    return authentication.principal as SecurityPrincipal
}

data class CoroutineAuthentication(
    val authentication: Authentication
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CoroutineAuthentication>
}

fun CoroutineContext.getAuthentication(): Authentication {
    return getAuthenticationOrNull()
        ?: throw IllegalStateException("Authentication is not set")
}

fun CoroutineContext.getAuthenticationOrNull(): Authentication? {
    val coroutineAuthentication = this[CoroutineAuthentication]
    return coroutineAuthentication?.authentication
}

private val dbContext = newFixedThreadPoolContext(10, "db-context")

suspend fun <T> withDbContext(block: suspend CoroutineScope.() -> T): T = withContext(dbContext, block)

suspend fun <T> withDbContextAsync(block: suspend CoroutineScope.() -> T): Deferred<T> = coroutineScope {
    async(context = dbContext, block = block)
}

fun voidMono(block: suspend CoroutineScope.() -> Unit): Mono<Void> = GlobalScope.mono {
    block()
    null
}