package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class Button<T : Any> private constructor(
    private val locator: Locator,
    private val parent: T,
) {
    fun shouldBeDisabled() = locator.assertThat().isDisabled()

    fun shouldBeEnabled() = locator.assertThat().isEnabled()

    operator fun invoke(action: Button<*>.() -> Unit): T {
        this.action()
        return parent
    }

    fun click() = locator.click()

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.buttonByText(label: String) =
            Button(page.getByText(label), this.owner)
    }
}
