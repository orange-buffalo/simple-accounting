package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

class Switch<P : Any> private constructor(
    private val locator: Locator,
    parent: P,
) : UiComponent<P, Switch<P>>(parent) {
    private val input = locator.locator("input")

    fun click() = locator.click()

    fun shouldBeVisible() = locator.shouldBeVisible()

    fun shouldBeSwitchedOn() {
        input.shouldSatisfy {
            getAttribute("aria-checked").shouldBe("true")
        }
    }

    fun shouldBeSwitchedOff() {
        input.shouldSatisfy {
            getAttribute("aria-checked").shouldBe("false")
        }
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
