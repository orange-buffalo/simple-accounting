package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath

class CurrencyInput private constructor(
    private val rootLocator: Locator,
) : UiComponent<CurrencyInput>() {
    private val select = Select.byContainer(rootLocator)
    private val input = rootLocator.locator(".el-select__wrapper")

    /**
     * Selects a currency by matching its innerText (e.g., "EUREuro").
     * Works with the custom currency option template that displays code and name separately.
     * The innerText format is "CODEName" with no separator (e.g., "EUREuro", "USDUS Dollar").
     */
    fun selectOption(currencyInnerText: String) {
        val popper = Popper.openOrLocateByTrigger(input)
        // Currency options have custom markup with separate spans for code and name
        // The innerText concatenates them without separator
        popper.rootLocator
            .locator("xpath=//*[${XPath.hasClass("el-select-dropdown__item")}][normalize-space(.)='$currencyInnerText']")
            .click()
        popper.shouldBeClosed()
    }

    fun shouldHaveSelectedValue(value: String) = select.shouldHaveSelectedValue(value)
    
    fun fill(text: String) {
        input.click()
        // Find the actual input element inside the wrapper for filtering
        val actualInput = input.locator("input.el-select__input")
        actualInput.fill(text)
    }

    fun shouldBeVisible() = select.shouldBeVisible()

    fun shouldBeHidden() = select.shouldBeHidden()

    fun shouldBeDisabled() = select.shouldBeDisabled()

    fun shouldHaveGroupedOptions(spec: (actualOptions: List<OptionsGroup>) -> Unit) =
        select.shouldHaveGroupedOptions(spec)

    companion object {
        fun byContainer(container: Locator) = CurrencyInput(container)
    }
}
