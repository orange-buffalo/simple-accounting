package io.orangebuffalo.accounting.simpleaccounting.services.integration

import kotlinx.coroutines.*
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import reactor.core.publisher.Mono
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

fun CoroutineScope.getCurrentPrincipal(): UserDetails {
    return coroutineContext.getPrincipal()
}

// todo #70: add user id to gwt token and user details so we do not need to load it from database for simple cases
fun CoroutineContext.getPrincipal(): UserDetails {
    val authentication = this.getAuthentication()
    return authentication.principal as UserDetails
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

fun CoroutineScope.getAuthentication(): Authentication {
    return coroutineContext.getAuthentication()
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
