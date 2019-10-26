package io.orangebuffalo.accounting.simpleaccounting.services.integration

import kotlinx.coroutines.*

private val dbContext = newFixedThreadPoolContext(10, "db-context")

suspend fun <T> withDbContext(block: suspend CoroutineScope.() -> T): T = withContext(dbContext, block)

suspend fun <T> withDbContextAsync(block: suspend CoroutineScope.() -> T): Deferred<T> = coroutineScope {
    async(context = dbContext, block = block)
}