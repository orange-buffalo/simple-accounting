package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldContainClass
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveAttribute

class WizardStep private constructor(private val container: Locator) : UiComponent<WizardStep>() {
    private val header = container.getByRole(AriaRole.BUTTON)

    fun click() = header.click()
    fun shouldBeDisabled() = header.shouldBeDisabled()
    fun shouldBeOpen() = header.shouldHaveAttribute(name = "aria-current", value = "step")
    fun shouldBeCompleted() = container.locator(".el-step__head").shouldContainClass("is-success")
    fun shouldBePending() = container.locator(".el-step__head").shouldContainClass("is-process")

    companion object {
        fun ComponentsAccessors.wizardStep(title: String) = WizardStep(
            page.locator(".el-step").filter(Locator.FilterOptions().setHasText(title)),
        )
    }
}
