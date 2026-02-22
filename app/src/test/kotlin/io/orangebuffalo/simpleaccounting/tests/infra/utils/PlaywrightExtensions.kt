package io.orangebuffalo.simpleaccounting.tests.infra.utils

import com.microsoft.playwright.ElementHandle
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Notifications
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaDocumentsList
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaIcon
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaMarkdownOutput
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.time.Duration.Companion.milliseconds

const val UI_ASSERTIONS_TIMEOUT_MS = 10_000
private val log = KotlinLogging.logger { }

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

private fun String.normalizeToNull(): String? = this
    .trim()
    // replace non-breaking spaces with regular spaces
    .replace('\u00A0', ' ')
    .ifBlank { null }

fun ElementHandle.innerTextOrNull(): String? = this.innerText().normalizeToNull()

fun Locator.innerTextOrNull(): String? = this.innerText().normalizeToNull()

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
fun shouldSatisfy(message: String? = null, spec: () -> Unit) = runBlocking {
    withClue(message ?: "Spec is not satisfied") {
        eventually(UI_ASSERTIONS_TIMEOUT_MS.milliseconds) {
            spec()
        }
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
    log.trace { "Starting API request blocking for $path" }
    var blockedRequestFailure: Throwable? = null
    var routeExecuted = false
    context().route("/api/$path") { route ->
        log.trace { "Route hit now: ${route.request().url()}" }
        routeExecuted = true
        try {
            blockedRequestSpec()
            log.run { "Blocked request spec executed" }
            route.resume()
            if (resetOnCompletion) {
                context().unroute("/api/$path")
            }
        } catch (e: Throwable) {
            log.trace { "Blocked request spec failed: ${e.message}, aborting the route" }
            blockedRequestFailure = e
            route.abort()
        }
    }

    log.trace { "Initiating the request initiator" }
    initiator()

    shouldSatisfy("The contract of this function requires the initiator to trigger the request") {
        // Playwright is synchronous, and route is hit only when we interact with the page;
        // hence, we execute the body assertion to ensure the route is eventually hit
        this.locator("body").shouldBeVisible()
        routeExecuted.shouldBeTrue()
    }

    // exceptions will not be automatically propagated from the route handler,
    // so we need to check if the blockedRequestSpec has thrown an exception,
    // so that the test receives the proper failure
    if (blockedRequestFailure != null) {
        throw blockedRequestFailure
    }
}

/**
 * Intercepts the GraphQL API call started by the [initiator]. Upon call execution,
 * blocks the response for the specified query or mutation and executes the provided [blockedRequestSpec].
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
 * @param queryOrMutationName the name of the GraphQL query or mutation to block
 * @param initiator a function that initiates the request
 * @param blockedRequestSpec a function that is executed when the request is blocked
 */
fun Page.withBlockedGqlApiResponse(
    queryOrMutationName: String,
    initiator: () -> Unit,
    blockedRequestSpec: () -> Unit,
    resetOnCompletion: Boolean = true,
) {
    log.trace { "Starting GraphQL API request blocking for $queryOrMutationName" }
    var blockedRequestFailure: Throwable? = null
    var routeExecuted = false
    context().route("/api/graphql") { route ->
        val postData = route.request().postData()
        if (postData != null && postData.contains("\"operationName\":\"$queryOrMutationName\"")) {
            log.trace { "Route hit for $queryOrMutationName: ${route.request().url()}" }
            routeExecuted = true
            try {
                blockedRequestSpec()
                log.trace { "Blocked request spec executed for $queryOrMutationName" }
                route.resume()
                if (resetOnCompletion) {
                    context().unroute("/api/graphql")
                }
            } catch (e: Throwable) {
                log.trace { "Blocked request spec failed: ${e.message}, aborting the route" }
                blockedRequestFailure = e
                route.abort()
            }
        } else {
            // Let other GraphQL requests pass through
            route.resume()
        }
    }

    log.trace { "Initiating the request initiator" }
    initiator()

    shouldSatisfy("The contract of this function requires the initiator to trigger the request") {
        // Playwright is synchronous, and route is hit only when we interact with the page;
        // hence, we execute the body assertion to ensure the route is eventually hit
        this.locator("body").shouldBeVisible()
        routeExecuted.shouldBeTrue()
    }

    // exceptions will not be automatically propagated from the route handler,
    // so we need to check if the blockedRequestSpec has thrown an exception,
    // so that the test receives the proper failure
    if (blockedRequestFailure != null) {
        throw blockedRequestFailure
    }
}

/**
 * Asserts that the locator satisfies the provided spec,
 * retrying the assertion until it succeeds or the timeout is reached.
 */
fun Locator.shouldSatisfy(message: String? = null, spec: Locator.() -> Unit) = runBlocking {
    withClue(message ?: "Spec is not satisfied on ($this)") {
        eventually(UI_ASSERTIONS_TIMEOUT_MS.milliseconds) {
            spec()
        }
    }
}

/**
 * Injects JavaScript utilities into a JavaScript snippet (typically, passed to [Locator.evaluate] or similar).
 */
fun injectJsUtils(): String = /* language=javascript */ $$"""
    // noinspection JSUnusedLocalSymbols
    const utils = {
        /**
          * For the given element, returns its text content trimmed to null.
          * Non-breaking spaces are replaced with regular spaces.
          * Noteworthy components data is extracted via their specific data extractors.
        */
        getDynamicContent: function(el) {
            if (!el) {
                return null;
            }

            // Check for specialized components
            const markdownValue = ($${SaMarkdownOutput.jsDataExtractor()})(el);
            if (markdownValue) {
                return markdownValue;
            }
            const documentsValue = ($${SaDocumentsList.jsDataExtractor()})(el);
            if (documentsValue) {
                return documentsValue;
            }
            
            let data = '';
            // Extract status value (has priority)
            const statusValue = ($${SaStatusLabel.jsDataExtractor()})(el);
            if (statusValue) {
                data += statusValue;
            } else {
                // SaStatusLabel has priority over SaIcon, as it can contain an icon inside
                data += ($${SaIcon.jsDataExtractor()})(el) || '';
            }
             
            const textContent = utils.transformTextContent(el.textContent);
            data += textContent || '';
            
            return data === '' ? null : data;
        },
      
        /**
        * Converts a visual value into a data extraction representation.
        * Must be in sync with the [visualToData] Kotlin function.
        */
        visualToData: (semantic, value) => {
            return `[${semantic}:${value}]`;
        },
        
        /**
        * Finds the closest descendant (including the element itself) that has the specified class.
        */
        findClosestByClass: function(el, className) {
            if (!el) {
                return null;
            }
            if (el.classList && el.classList.contains(className)) {
                return el;
            }
            const descendants = el.getElementsByClassName(className);
            return descendants.length > 0 ? descendants[0] : null;
        },
        
        /**
        * Transforms text content by replacing non-breaking spaces and trimming.
        */
        transformTextContent: function(textContent) {
            if (!textContent) {
                return null;
            }
            // replace non-breaking spaces with regular spaces
            textContent = textContent.replace(/\u00A0/g, ' ');
            // trim
            textContent = textContent.trim();
            return textContent === '' ? null : textContent;
        }
    };
"""

/**
 * A helper function for consistent representation of visual values in data extraction JavaScript.
 * Intended to be used by components implementation, not by tests directly.
 */
fun visualToData(semantic: String, value: String): String = "[$semantic:$value]"

/**
 * A helper function to concatenate multiple data values into a single string,
 * as used in data extraction JavaScript.
 * Intended to be used by tests to provide the expected values.
 */
fun dataValues(vararg values: String): String = values.joinToString("")
