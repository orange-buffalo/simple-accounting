package io.orangebuffalo.simpleaccounting.tests.infra.utils

import com.microsoft.playwright.ElementHandle
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.booleans.shouldBeTrue
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Notifications
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

const val UI_ASSERTIONS_TIMEOUT_MS = 10_000

object XPath {
    fun hasClass(className: String): String = "contains(concat(' ', normalize-space(@class), ' '), ' $className ')"

    fun h1WithText(text: String): String = "//h1[normalize-space(.) = '$text']"

    fun hasText(text: String): String = "normalize-space(.) = '$text'"
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
    eventually(UI_ASSERTIONS_TIMEOUT_MS.milliseconds) {
        spec()
    }
}

/**
 * Intercepts the API call started by the [initiator]. Upon call execution,
 * blocks the response and executes the provided [blockedRequestSpec].
 * Then resumes the request with the original response. The method is blocked
 * until both the [initiator] and [blockedRequestSpec] are executed.
 *
 * **Important**: [initiator] must invoke an assertion on a locator that ensures the request is initiated;
 * without this, the [blockedRequestSpec] will be only invoked on the next locator interaction, which
 * might be far away from the request initiation and cause unstable behavior.
 *
 * Both callbacks are executed _in the same thread_ due to the Playwright sync API design. This
 * is the reason for this function to have two parameters instead of one.
 *
 * The motivation of this function is to provide clear visibility on how the blocked request
 * is related to pre- and post-processing.
 *
 * @param path relative to the API root, without leading slash
 * @param initiator a function that initiates the request
 * @param blockedRequestSpec a function that is executed when the request is blocked
 */
fun Page.withBlockedApiResponse(
    path: String,
    initiator: () -> Unit,
    blockedRequestSpec: () -> Unit,
    resetOnCompletion: Boolean = true,
) {
    var blockedRequestFailure: Throwable? = null
    var blockedRequestExecuted = false
    context().route("/api/$path") { route ->
        try {
            blockedRequestSpec()
            blockedRequestExecuted = true
            route.resume()
            if (resetOnCompletion) {
                context().unroute("/api/$path")
            }
        } catch (e: Throwable) {
            blockedRequestFailure = e
            route.abort()
        }
    }
    // Playwright is synchronous, so once we call this, the route should be hit (by the contract of this function);
    // after this call, the blockedRequestSpec will be executed.
    initiator()

    // exceptions will not be automatically propagated from the route handler,
    // so we need to check if the blockedRequestSpec has thrown an exception,
    // so that the test receives the proper failure
    if (blockedRequestFailure != null) {
        throw blockedRequestFailure!!
    }

    withHint("The contract of this function requires the initiator to trigger the request") {
        blockedRequestExecuted.shouldBeTrue()
    }
}

/**
 * Asserts that the locator satisfies the provided spec,
 * retrying the assertion until it succeeds or the timeout is reached.
 */
fun Locator.shouldSatisfy(message: String? = null, spec: Locator.() -> Unit) =
    withHint(message ?: "Spec is not satisfied on ($this)") {
        eventually(UI_ASSERTIONS_TIMEOUT_MS.milliseconds) {
            spec()
        }
    }
