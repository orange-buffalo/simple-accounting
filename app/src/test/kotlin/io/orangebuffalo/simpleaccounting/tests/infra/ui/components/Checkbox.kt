package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeChecked
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled

class Checkbox<P : Any> private constructor(
    private val locator: Locator,
    parent: P,
) : UiComponent<P, Checkbox<P>>(parent) {
    fun shouldBeChecked() = locator.shouldBeChecked()
    fun shouldBeEnabled() = locator.shouldBeEnabled()
    fun shouldBeDisabled() = locator.shouldBeDisabled()

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.checkboxByOwnLabel(label: String) =
            Checkbox(page.getByLabel(label), this.owner)
    }
}
