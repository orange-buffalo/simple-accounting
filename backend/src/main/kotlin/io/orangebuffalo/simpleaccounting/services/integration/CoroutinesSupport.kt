package io.orangebuffalo.simpleaccounting.services.integration

import kotlinx.coroutines.*
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.web.server.ServerWebExchange
import reactor.util.context.Context
import kotlin.coroutines.coroutineContext

@Suppress("EXPERIMENTAL_API_USAGE")
private val dbContext = newFixedThreadPoolContext(20, "db-context")

suspend fun <T> withDbContext(block: suspend CoroutineScope.() -> T): T = withContext(dbContext, block)

suspend fun <T> withDbContextAsync(block: suspend CoroutineScope.() -> T): Deferred<T> = coroutineScope {
    async(context = dbContext, block = block)
}

@Suppress("EXPERIMENTAL_API_USAGE")
suspend fun getReactorContext(): Context {
    val reactorContext: ReactorContext = coroutineContext[ReactorContext]
        ?: throw IllegalArgumentException("Cannot find reactor context")
    return reactorContext.context
}

suspend fun getServerWebExchange(): ServerWebExchange =
    getReactorContext().getOrEmpty<ServerWebExchange>(ServerWebExchange::class.java)
        .orElseThrow { IllegalStateException("ServerWebExchange is not found") }

suspend fun executeInParallel(spec: ParallelExecutionSpec.() -> Unit) = coroutineScope {
    val steps = mutableListOf<suspend () -> Unit>()
    val parallelRunSpec = object : ParallelExecutionSpec {
        override fun step(stepSpec: suspend () -> Unit) {
            steps.add(stepSpec)
        }
    }
    spec(parallelRunSpec)
    awaitAll(*steps.map { step -> async { step() } }.toTypedArray())
}

interface ParallelExecutionSpec {
    fun step(stepSpec: suspend () -> Unit)
}
