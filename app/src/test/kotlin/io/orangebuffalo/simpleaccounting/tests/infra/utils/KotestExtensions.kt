package io.orangebuffalo.simpleaccounting.tests.infra.utils

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
 * Executes the given spec on current object with the provided clue.
 * Same [withClue], but is an extension function that allows using it in a call chain.
 */
fun <T> T.shouldWithClue(clue: String, spec: T.() -> Unit): T {
    withClue(clue) {
        this.should(spec)
    }
    return this
}

/**
 * Executes the given action on current object with the provided clue.
 * Same [withClue], but is an extension function that allows using it in a call chain.
 */
fun <T : Any?, R> T.withHint(
    message: String,
    action: T.() -> R
): R = withClue(message) {
    this.action()
}

/**
 * Repeats the assertions until satisfied or timeout is reached.
 */
fun <T> shouldEventually(message: String? = null, spec: () -> T): T = runBlocking {
    withClue(message ?: "Spec is not satisfied on ($this)") {
        eventually(10.seconds) {
            spec()
        }
    }
}
