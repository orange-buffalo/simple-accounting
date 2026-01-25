package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeChecked
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled

class Checkbox private constructor(
    private val locator: Locator,
) : UiComponent<Checkbox>() {
    fun shouldBeChecked() = locator.shouldBeChecked()
    fun shouldBeEnabled() = locator.shouldBeEnabled()
    fun shouldBeDisabled() = locator.shouldBeDisabled()
    fun click() = locator.click()

    companion object {
        fun ComponentsAccessors.checkboxByOwnLabel(label: String) =
            Checkbox(page.getByLabel(label))

        fun checkboxByOwnLabel(container: Locator, label: String) =
            Checkbox(container.getByLabel(label))
    }
}
