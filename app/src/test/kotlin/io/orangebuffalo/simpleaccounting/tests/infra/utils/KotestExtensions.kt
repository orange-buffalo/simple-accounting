package io.orangebuffalo.simpleaccounting.tests.infra.utils

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.should
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies that the iterable contains exactly one element and returns it
 */
fun <T> Iterable<T>.shouldBeSingle(): T = shouldBeSingleton().single()

/**
 * Executes the given spec on current object with the provided clue
 */
fun <T> T.shouldWithHint(clue: String, spec: T.() -> Unit): T {
    withHint(clue) {
        this.should(spec)
    }
    return this
}

/**
 * Repeats the assertions until satisfied or timeout is reached.
 */
fun <T> shouldEventually(message: String? = null, spec: () -> T): T = runBlocking {
    withHint(message ?: "Spec is not satisfied on ($this)") {
        eventually(10.seconds) {
            spec()
        }
    }
}

/**
 * `withClue` of Kotest is only integrated with Kotest assertions,
 * and if the closure fails with anything else (e.g. an exception),
 * it does not provide the proper clue. This function is a workaround
 * to provide a clue for any exception thrown during the execution of the action.
 */
fun <T> withHint(
    message: String? = null,
    action: suspend () -> T
): T = try {
    runBlocking { action() }
} catch (e: Throwable) {
    logger.error(e) { "'$message' failed" }
    throw e
}
