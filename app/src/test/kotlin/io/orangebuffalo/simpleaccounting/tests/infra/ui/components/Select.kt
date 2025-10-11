package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldContainClass
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*

class Select private constructor(
    rootLocator: Locator,
) : UiComponent<Select>() {
    private val input = rootLocator.locator(".el-select__wrapper")

    fun shouldBeVisible() = input.shouldBeVisible()

    fun shouldBeHidden() = input.shouldBeHidden()

    fun selectOption(option: String) {
        val popper = Popper.openOrLocateByTrigger(input)
        popper.rootLocator
            .locator("xpath=//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='$option']")
            .click()
        popper.shouldBeClosed()
        shouldHaveSelectedValue(option)
    }

    /**
     * Selects an option without validating the selected value afterward.
     * Useful when the selection triggers immediate side effects that change the form structure
     * (e.g., changing UI language makes form labels change, breaking the validation locator).
     */
    fun selectOptionWithoutValidation(option: String) {
        val popper = Popper.openOrLocateByTrigger(input)
        popper.rootLocator
            .locator("xpath=//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='$option']")
            .click()
        popper.shouldBeClosed()
    }

    fun shouldHaveSelectedValue(value: String) {
        // Exclude aria-hidden spans (e.g., el-select__input-calculator for filterable selects)
        input.locator("xpath=//*[${XPath.hasClass("el-select__selected-item")}]/span[not(@aria-hidden)]")
            .shouldHaveText(value)
    }

    fun shouldHaveOptions(vararg options: String) {
        shouldHaveOptions { actualOptions ->
            actualOptions.shouldWithClue("Expected options: $options") {
                shouldContainExactly(*options)
            }
        }
    }

    fun shouldHaveOptions(spec: (actualOptions: List<String>) -> Unit) {
        val popper = Popper.openOrLocateByTrigger(input)
        popper.rootLocator.shouldSatisfy {
            locator("xpath=//*[${XPath.hasClass("el-select-dropdown__item")}]")
                .allInnerTexts()
                .should(spec)
        }
    }

    fun shouldHaveGroupedOptions(spec: (actualOptions: List<OptionsGroup>) -> Unit) {
        val popper = Popper.openOrLocateByTrigger(input)
        popper.rootLocator.shouldSatisfy {
            // JS evaluation for performance reasons
            // language=javascript
            val actualGroupsJson = evaluate(
                """
                (popper) => JSON.stringify(Array.from(popper
                    .querySelectorAll('.el-select-group__wrap'))
                    .map(group => ({
                        name: group.querySelector('.el-select-group__title').innerText,
                        options: Array.from(group.querySelectorAll('.el-select-dropdown__item'))
                            .map(option => option.innerText)
                    })))
            """
            ) as String
            val actualGroups = Gson().fromJson(actualGroupsJson, object : TypeToken<List<OptionsGroup>>() {})
            spec(actualGroups)
        }
    }

    fun shouldBeDisabled() {
        input.shouldContainClass("is-disabled")
    }

    companion object {
        fun byContainer(container: Locator) = Select(container)
    }
}

data class OptionsGroup(val name: String, val options: List<String>)
