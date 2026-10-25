package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.orangebuffalo.kotestplaywrightassertions.shouldContainClass
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveAttribute
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount

class WizardStep private constructor(private val container: Locator) : UiComponent<WizardStep>() {
    private val title = container.locator(".el-step__title span")

    fun shouldBeNonInteractive() = container.getByRole(AriaRole.BUTTON).shouldHaveCount(0)
    fun shouldBeOpen() = title.shouldHaveAttribute(name = "aria-current", value = "step")
    fun shouldBeCompleted() = container.locator(".el-step__head").shouldContainClass("is-success")
    fun shouldBePending() = container.locator(".el-step__head").shouldContainClass("is-process")

    companion object {
        fun ComponentsAccessors.wizardStep(title: String) = WizardStep(
            page.locator(".el-step").filter(Locator.FilterOptions().setHasText(title)),
        )
    }
}
