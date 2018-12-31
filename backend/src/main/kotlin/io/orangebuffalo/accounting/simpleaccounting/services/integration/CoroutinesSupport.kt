package io.orangebuffalo.accounting.simpleaccounting.services.integration

import kotlinx.coroutines.*
import org.springframework.security.core.userdetails.UserDetails
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

fun CoroutineScope.getCurrentPrincipal(): UserDetails {
    return coroutineContext.getPrincipal()
}

// todo add user id to gwt token and user details so we do not need to load it from database for simple cases
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
