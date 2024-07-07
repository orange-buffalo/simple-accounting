package io.orangebuffalo.simpleaccounting.infra.utils

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
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
fun <T> T.shouldWithClue(clue: String, spec: T.() -> Unit): T {
    withClue(clue) {
        this.should(spec)
    }
    return this
}

/**
 * Repeats the assertions until satisfied or timeout is reached.
 */
fun <T> shouldEventually(message: String? = null, spec: () -> T) : T = runBlocking {
    withClue(message ?: "Spec is not satisfied on ($this)") {
        eventually(10.seconds) {
            spec()
        }
    }
}
