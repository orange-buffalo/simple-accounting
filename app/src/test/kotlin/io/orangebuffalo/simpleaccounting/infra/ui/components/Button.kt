package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.utils.assert
import io.orangebuffalo.simpleaccounting.infra.utils.shouldBeVisible
import io.orangebuffalo.simpleaccounting.infra.utils.shouldNotBeVisible

class Button<P : Any> private constructor(
    private val locator: Locator,
    parent: P,
) : UiComponent<P, Button<P>>(parent) {
    fun shouldBeDisabled() = locator.assert { isDisabled() }

    fun shouldBeEnabled() = locator.assert { isEnabled() }

    fun click() = locator.click()

    fun shouldBeVisible() = locator.shouldBeVisible()

    fun shouldNotBeVisible() = locator.shouldNotBeVisible()

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.buttonByText(label: String) =
            Button(page.getByText(label), this.owner)
    }
}
