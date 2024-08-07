package io.orangebuffalo.simpleaccounting.infra

import kotlinx.coroutines.*
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.web.server.ServerWebExchange
import reactor.util.context.Context
import kotlin.coroutines.coroutineContext

@OptIn(DelicateCoroutinesApi::class)
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

/**
 * Executes all the steps in parallel and waits for the completion. Always waits for all
 * steps to complete. Fails on the first exceptionally completed step in the order as the steps
 * were configured (unlike [awaitAll]).
 */
suspend fun executeInParallel(spec: ParallelExecutionSpec.() -> Unit) = coroutineScope {
    val steps = mutableListOf<suspend () -> Unit>()
    val parallelRunSpec = object : ParallelExecutionSpec {
        override fun step(stepSpec: suspend () -> Unit) {
            steps.add(stepSpec)
        }
    }

    spec(parallelRunSpec)

    steps.asSequence()
        .map { step -> async { step() } }
        .forEach { stepDeferred -> stepDeferred.await() }
}

interface ParallelExecutionSpec {
    fun step(stepSpec: suspend () -> Unit)
}
