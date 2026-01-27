package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeChecked
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled

class Checkbox private constructor(
    private val locator: Locator,
) : UiComponent<Checkbox>() {
    fun shouldBeChecked() = locator.shouldBeChecked()
    fun shouldBeEnabled() = locator.shouldBeEnabled()
    fun shouldBeDisabled() = locator.shouldBeDisabled()
    fun click() {
        // Element Plus checkboxes have aria-hidden input
        // Use getByRole to find the actual clickable checkbox element
        locator.click()
    }

    companion object {
        fun ComponentsAccessors.checkboxByOwnLabel(label: String) =
            // Find the checkbox by its visible label text, not the hidden input
            Checkbox(page.getByRole(com.microsoft.playwright.options.AriaRole.CHECKBOX, Page.GetByRoleOptions().setName(label)))

        fun checkboxByOwnLabel(container: Locator, label: String) =
            Checkbox(container.getByRole(com.microsoft.playwright.options.AriaRole.CHECKBOX, Locator.GetByRoleOptions().setName(label)))
    }
}
