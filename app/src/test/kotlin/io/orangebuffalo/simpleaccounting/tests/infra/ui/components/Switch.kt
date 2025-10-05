package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
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
        /**
         * Assumes a single switch in the container and returns it
         */
        fun ComponentsAccessors.switchByContainer(container: Locator) =
            Switch(container.locator(".el-switch"))
    }
}
