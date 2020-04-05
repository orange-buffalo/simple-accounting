package io.orangebuffalo.simpleaccounting.services.integration

import kotlinx.coroutines.*

@Suppress("EXPERIMENTAL_API_USAGE")
private val dbContext = newFixedThreadPoolContext(20, "db-context")

suspend fun <T> withDbContext(block: suspend CoroutineScope.() -> T): T = withContext(dbContext, block)

suspend fun <T> withDbContextAsync(block: suspend CoroutineScope.() -> T): Deferred<T> = coroutineScope {
    async(context = dbContext, block = block)
}
