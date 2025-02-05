package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*

class Select<P : Any> private constructor(
    rootLocator: Locator,
    parent: P,
) : UiComponent<P, Select<P>>(parent) {
    private val input = rootLocator.locator(".el-select__wrapper")

    fun shouldBeVisible() = input.shouldBeVisible()

    fun shouldNotBeVisible() = input.shouldBeHidden()

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
            @Suppress("UNCHECKED_CAST")
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
        input.shouldHaveClass("is-disabled")
    }

    companion object {
        fun byContainer(container: Locator) = Select(container, Unit)
    }
}

data class OptionsGroup(val name: String, val options: List<String>)
