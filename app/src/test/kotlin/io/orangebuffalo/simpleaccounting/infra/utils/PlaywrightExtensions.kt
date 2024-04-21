package io.orangebuffalo.simpleaccounting.infra.utils

import com.microsoft.playwright.ElementHandle
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.LocatorAssertions
import com.microsoft.playwright.assertions.PlaywrightAssertions
import io.kotest.assertions.nondeterministic.eventually
import io.orangebuffalo.simpleaccounting.infra.ui.components.Notifications
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

@Deprecated("Use PlaywrightAssertions instead")
fun Locator.assertThat(): LocatorAssertions = PlaywrightAssertions.assertThat(this)

@Deprecated("Use PlaywrightAssertions instead")
fun Locator.assert(spec: LocatorAssertions.(element: Locator) -> Unit) {
    spec(PlaywrightAssertions.assertThat(this), this)
}

object XPath {
    fun hasClass(className: String): String = "contains(concat(' ', normalize-space(@class), ' '), ' $className ')"

    fun h1WithText(text: String): String = "//h1[normalize-space(.) = '$text']"
}

fun Page.navigateAndDisableAnimations(path: String): Page {
    navigate(path)
    addStyleTag(
        // disable animations to speedup the tests
        Page.AddStyleTagOptions()
            .setContent(
                """*, *::before, *::after {
                  transition-duration: 0s !important;
                  transition-delay: 0s !important;
                  animation-duration: 0s !important;
                  animation-delay: 0s !important;
                }"""
            )
    )
    return this
}

fun ElementHandle.hasClass(className: String): Boolean = evaluate(
    """function(node, className) {
        return node.classList.contains(className);
    }""",
    className
) as Boolean

fun ElementHandle.innerTextOrNull(): String? = this.innerText().trim().ifBlank { null }

fun Locator.innerTextOrNull(): String? = this.innerText().trim().ifBlank { null }

fun Locator.innerTextTrimmed() = this.innerText().trim()

fun Page.shouldHaveNotifications(spec: Notifications.() -> Unit) {
    Notifications(this).spec()
}

/**
 * Awaits until the provided spec is satisfied.
 * Spec is satisfied when it does not throw an assertion error.
 *
 * Playwright does not have a built-in mechanism to wait for a condition to be satisfied,
 * hence using Kotest.
 */
fun shouldSatisfy(spec: () -> Unit) = runBlocking {
    eventually(10.seconds) {
        spec()
    }
}
