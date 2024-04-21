package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.utils.shouldHaveClass
import io.orangebuffalo.simpleaccounting.infra.utils.shouldHaveText

/**
 * A wrapper around `SaStatusLabel` component.
 */
class SaStatusLabel<T : Any> private constructor(
    private val container: Locator,
    parent: T,
) : UiComponent<T, SaStatusLabel<T>>(parent) {

    /**
     * Asserts that this label is of error (failure) state.
     * If [content] is not null, also asserts that the alert contains the given content.
     */
    fun shouldBeError(content: String?) {
        container.shouldHaveClass("sa-status-label_failure")
        if (content != null) {
            container.shouldHaveText(content)
        }
    }

    /**
     * Asserts that this label is of regular state.
     * If [content] is not null, also asserts that the alert contains the given content.
     */
    fun shouldBeRegular(content: String?) {
        container.shouldHaveClass("sa-status-label_regular")
        if (content != null) {
            container.shouldHaveText(content)
        }
    }

    /**
     * Asserts that this label is of success state.
     * If [content] is not null, also asserts that the alert contains the given content.
     */
    fun shouldBeSuccess(content: String?) {
        container.shouldHaveClass("sa-status-label_success")
        if (content != null) {
            container.shouldHaveText(content)
        }
    }

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.statusLabel(container: Locator? = null): SaStatusLabel<T> {
            val selector = ".sa-status-label"
            return SaStatusLabel(
                container = if (container == null)
                    page.locator(selector) else container.locator(selector),
                parent = this.owner
            )
        }
    }
}
