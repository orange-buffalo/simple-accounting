package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveAttribute

class Switch<P : Any> private constructor(
    private val locator: Locator,
    parent: P,
) : UiComponent<P, Switch<P>>(parent) {
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
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.switchByContainer(container: Locator) =
            Switch(container.locator(".el-switch"), this.owner)
    }
}
