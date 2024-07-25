package io.orangebuffalo.simpleaccounting.tests.infra.utils

import com.microsoft.playwright.Locator
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/**
 * Asserts that the locator satisfies the provided spec,
 * retrying the assertion until it succeeds or the timeout is reached.
 */
fun Locator.shouldSatisfy(message: String? = null, spec: Locator.() -> Unit) = runBlocking {
    withClue(message ?: "Spec is not satisfied on ($this)") {
        eventually(10.seconds) {
            spec()
        }
    }
}

/**
 * Asserts that the locator has the provided class. It does not require the class to be the only one.
 */
fun Locator.shouldHaveClass(className: String) = this.shouldSatisfy(
    "Element does not have class '$className'"
) {
    this.elementHandle().hasClass(className).shouldBeTrue()
}

/**
 * Asserts that the locator has the provided text.
 */
fun Locator.shouldHaveText(text: String) = this.shouldSatisfy(
    "Element does not have text '$text'"
) {
    this.innerTextTrimmed().shouldBe(text)
}

/**
 * Asserts that the locator is visible.
 */
fun Locator.shouldBeVisible() = this.shouldSatisfy(
    "Element is not visible"
) {
    this.isVisible.shouldBeTrue()
}

/**
 * Asserts that the locator is hidden.
 */
fun Locator.shouldNotBeVisible() = this.shouldSatisfy(
    "Element is not hidden"
) {
    this.isHidden.shouldBeTrue()
}
