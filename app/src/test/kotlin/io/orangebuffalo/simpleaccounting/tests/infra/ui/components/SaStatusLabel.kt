package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldContainClass
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText

/**
 * A wrapper around `SaStatusLabel` component.
 */
class SaStatusLabel private constructor(
    private val container: Locator,
) : UiComponent<SaStatusLabel>() {

    /**
     * Asserts that this label is of error (failure) state.
     * If [content] is not null, also asserts that the alert contains the given content.
     */
    fun shouldBeError(content: String? = null) {
        container.shouldContainClass("sa-status-label_failure")
        if (content != null) {
            container.shouldHaveText(content)
        }
    }

    /**
     * Asserts that this label is of regular state.
     * If [content] is not null, also asserts that the alert contains the given content.
     */
    fun shouldBeRegular(content: String? = null) {
        container.shouldContainClass("sa-status-label_regular")
        if (content != null) {
            container.shouldHaveText(content)
        }
    }

    /**
     * Asserts that this label is of pending state.
     * If [content] is not null, also asserts that the alert contains the given content.
     */
    fun shouldBePending(content: String? = null) {
        container.shouldContainClass("sa-status-label_pending")
        if (content != null) {
            container.shouldHaveText(content)
        }
    }

    /**
     * Asserts that this label is of success state.
     * If [content] is not null, also asserts that the alert contains the given content.
     */
    fun shouldBeSuccess(content: String?) {
        container.shouldContainClass("sa-status-label_success")
        if (content != null) {
            container.shouldHaveText(content)
        }
    }

    /**
     * Asserts that this label is of simplified success state.
     * If [content] is not null, also asserts that the alert contains the given content.
     */
    fun shouldBeSimplifiedSuccess(content: String?) {
        container.shouldContainClass("sa-status-label_success-simplified")
        if (content != null) {
            container.shouldHaveText(content)
        }
    }

    /**
     * Asserts that this label is of simplified pending state.
     * If [content] is not null, also asserts that the alert contains the given content.
     */
    fun shouldBeSimplifiedPending(content: String?) {
        container.shouldContainClass("sa-status-label_pending-simplified")
        if (content != null) {
            container.shouldHaveText(content)
        }
    }

    companion object {
        fun ComponentsAccessors.statusLabel(container: Locator? = null): SaStatusLabel {
            val selector = ".sa-status-label"
            return SaStatusLabel(
                container = if (container == null)
                    page.locator(selector) else container.locator(selector)
            )
        }
    }
}
