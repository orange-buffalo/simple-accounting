package io.orangebuffalo.accounting.simpleaccounting.services.business

import kotlinx.coroutines.*
import org.springframework.security.core.userdetails.UserDetails
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

fun CoroutineScope.getCurrentPrincipal(): UserDetails {
    return coroutineContext.getPrincipal()
}

fun CoroutineContext.getPrincipal(): UserDetails {
    val coroutinePrincipal = this[CoroutinePrincipal] ?: throw IllegalStateException("Principal is not set")
    return coroutinePrincipal.principal
}

data class CoroutinePrincipal(
    val principal: UserDetails
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CoroutinePrincipal>
}

private val dbContext = newFixedThreadPoolContext(10, "db-context")

suspend fun <T> withDbContext(block: suspend CoroutineScope.() -> T) : T = withContext(dbContext, block)

suspend fun <T> withDbContextAsync(block: suspend CoroutineScope.() -> T) : Deferred<T> = coroutineScope {
    async(context = dbContext, block =  block)
}
