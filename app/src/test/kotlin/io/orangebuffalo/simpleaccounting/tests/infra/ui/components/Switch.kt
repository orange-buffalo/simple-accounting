package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveAttribute

class Switch private constructor(
    private val locator: Locator,
) : UiComponent<Switch>() {
    private val input = locator.locator("input")

    fun click() = locator.click()

    fun shouldBeVisible() = locator.shouldBeVisible()

    fun shouldBeSwitchedOn() {
        input.shouldHaveAttribute("aria-checked", "true")
    }

    fun shouldBeSwitchedOff() {
        input.shouldHaveAttribute("aria-checked", "false")
    }

    fun toggle() {
        locator.click()
    }

    companion object {
        fun ComponentsAccessors.switchSectionByLabel(label: String) = switchByContainer(
            page.locator(".sa-form-switch-section").filter(Locator.FilterOptions().setHas(
                page.getByRole(AriaRole.HEADING, Page.GetByRoleOptions().setName(label).setExact(true))
            ))
        )

        /**
         * Assumes a single switch in the container and returns it
         */
        fun ComponentsAccessors.switchByContainer(container: Locator) =
            Switch(container.locator(".el-switch"))
    }
}
