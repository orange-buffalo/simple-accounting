package io.orangebuffalo.simpleaccounting.infra.utils

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.should

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
