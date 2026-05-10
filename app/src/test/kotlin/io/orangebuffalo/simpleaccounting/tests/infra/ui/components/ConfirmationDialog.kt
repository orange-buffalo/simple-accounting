package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible

class ConfirmationDialog private constructor(
    private val locator: Locator,
) : UiComponent<ConfirmationDialog>() {

    fun shouldBeVisible(): ConfirmationDialog {
        locator.shouldBeVisible()
        return this
    }

    fun clickButton(label: String): ConfirmationDialog {
        locator.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName(label).setExact(true)).click()
        return this
    }

    companion object {
        fun ComponentsAccessors.confirmationDialog() = ConfirmationDialog(page.getByRole(AriaRole.DIALOG))
    }
}
