package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeChecked
import io.orangebuffalo.kotestplaywrightassertions.shouldNotBeChecked
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden

class Checkbox private constructor(
    private val locator: Locator,
) : UiComponent<Checkbox>() {
    fun shouldBeChecked() = locator.shouldBeChecked()
    fun shouldNotBeChecked() = locator.shouldNotBeChecked()
    fun shouldBeEnabled() = locator.shouldBeEnabled()
    fun shouldBeDisabled() = locator.shouldBeDisabled()
    fun shouldBeVisible() = locator.shouldBeVisible()
    fun shouldBeHidden() = locator.shouldBeHidden()
    
    fun check() = locator.check()
    
    fun click() {
        // Element Plus renders checkboxes in different ways:
        // 1. With text: wrapped in label.el-checkbox
        // 2. Without text: just a bare input[type='checkbox']
        // Try to find the parent label first, fallback to clicking the input directly
        try {
            val checkboxContainer = locator.locator("xpath=ancestor::label[contains(@class, 'el-checkbox')]").first()
            checkboxContainer.scrollIntoViewIfNeeded()
            checkboxContainer.click()
        } catch (e: com.microsoft.playwright.TimeoutError) {
            // No .el-checkbox container found, click the input directly
            locator.scrollIntoViewIfNeeded()
            locator.click()
        }
    }

    companion object {
        fun ComponentsAccessors.checkboxByOwnLabel(label: String) =
            // Find the checkbox by its visible label text, not the hidden input
            Checkbox(page.getByRole(com.microsoft.playwright.options.AriaRole.CHECKBOX, Page.GetByRoleOptions().setName(label)))

        fun checkboxByOwnLabel(container: Locator, label: String) =
            Checkbox(container.getByRole(com.microsoft.playwright.options.AriaRole.CHECKBOX, Locator.GetByRoleOptions().setName(label)))

        fun byContainer(container: Locator) =
            Checkbox(container.locator(".el-checkbox__input input, input[type='checkbox']").first())
    }
}
