package io.orangebuffalo.simpleaccounting.tests.infra.utils

import com.microsoft.playwright.Locator
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import io.kotest.assertions.nondeterministic.eventually
import kotlin.time.Duration.Companion.seconds

/**
 * Asserts that the locator satisfies the provided spec,
 * retrying the assertion until it succeeds or the timeout is reached.
 */
fun Locator.shouldSatisfy(message: String? = null, spec: Locator.() -> Unit) =
    withHint(message ?: "Spec is not satisfied on ($this)") {
        eventually(10.seconds) {
            spec()
        }
        assertThat(this)
    }
