package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*

class Select<P : Any> private constructor(
    rootLocator: Locator,
    parent: P,
) : UiComponent<P, Select<P>>(parent) {
    private val input = rootLocator.locator(".el-select__wrapper")

    fun shouldBeVisible() = input.shouldBeVisible()

    fun shouldNotBeVisible() = input.shouldNotBeVisible()

    fun selectOption(option: String) {
        val popper = Popper.openOrLocateByTrigger(input)
        popper.rootLocator
            .locator("xpath=//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='$option']")
            .click()
        popper.shouldBeClosed()
        shouldHaveSelectedValue(option)
    }

    fun shouldHaveSelectedValue(value: String) {
        input.locator("xpath=//*[${XPath.hasClass("el-select__selected-item")}]/span").shouldHaveText(value)
    }

    fun shouldHaveOptions(vararg options: String) {
        val popper = Popper.openOrLocateByTrigger(input)
        popper.rootLocator.shouldSatisfy {
            locator("xpath=//*[${XPath.hasClass("el-select-dropdown__item")}]/span")
                .allInnerTexts()
                .shouldWithClue("Expected options: $options") {
                    shouldContainExactly(*options)
                }
        }
    }

    fun shouldBeDisabled() {
        input.shouldHaveClass("is-disabled")
    }

    companion object {
        fun byContainer(container: Locator) = Select(container, Unit)
    }
}
