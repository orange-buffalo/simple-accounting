package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldContainClass
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.utils.visualToData

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
        private const val VISUAL_SEMANTIC = "status"

        fun ComponentsAccessors.statusLabel(container: Locator? = null): SaStatusLabel {
            val selector = ".sa-status-label"
            return SaStatusLabel(
                container = if (container == null)
                    page.locator(selector) else container.locator(selector)
            )
        }

        /**
         * Tests can use this method to produce regular status data value.
         * This is used for JS-based data extractors, like [SaPageableItems].
         */
        fun regularStatusValue(): String = visualToData(VISUAL_SEMANTIC, "regular")

        /**
         * Tests can use this method to produce error status data value.
         * This is used for JS-based data extractors, like [SaPageableItems].
         */
        fun errorStatusValue(): String = visualToData(VISUAL_SEMANTIC, "failure")

        /**
         * Tests can use this method to produce pending status data value.
         * This is used for JS-based data extractors, like [SaPageableItems].
         */
        fun pendingStatusValue(): String = visualToData(VISUAL_SEMANTIC, "pending")

        /**
         * Tests can use this method to produce success status data value.
         * This is used for JS-based data extractors, like [SaPageableItems].
         */
        fun successStatusValue(): String = visualToData(VISUAL_SEMANTIC, "success")

        /**
         * JavaScript function that extracts the status data value from any element inside the status label component.
         * Used in JS-based data extractors, like [SaPageableItems], indirectly via
         * [io.orangebuffalo.simpleaccounting.tests.infra.utils.injectJsUtils] `getDynamicContent`.
         * Not intended for direct use in tests.
         */
        fun jsDataExtractor() = /* language=JavaScript */ """
            (anyElement) => {
                const statusLabelElement = utils.findClosestByClass(anyElement, 'sa-status-label');
                if (!statusLabelElement) {
                    return null;
                }
                const statusClasses = Array.from(statusLabelElement.classList);
                let statusValue = null;
                if (statusClasses.includes('sa-status-label_failure')) {
                    statusValue = 'failure';
                } else if (statusClasses.includes('sa-status-label_regular')) {
                    statusValue = 'regular';
                } else if (statusClasses.includes('sa-status-label_pending')) {
                    statusValue = 'pending';
                } else if (statusClasses.includes('sa-status-label_success')) {
                    statusValue = 'success';
                }
                if (!statusValue) {
                    return null;
                }
                return utils.visualToData('$VISUAL_SEMANTIC', statusValue);
            }   
        """
    }
}
