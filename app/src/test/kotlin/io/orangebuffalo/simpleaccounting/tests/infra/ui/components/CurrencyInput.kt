package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath

class CurrencyInput private constructor(
    private val rootLocator: Locator,
) : UiComponent<CurrencyInput>() {
    private val select = Select.byContainer(rootLocator)
    private val input = rootLocator.locator(".el-select__wrapper")

    /**
     * Selects a currency by its label (e.g., "EUR - Euro").
     * Works with the custom currency option template that displays code and name separately.
     */
    fun selectOption(currencyLabel: String) {
        val popper = Popper.openOrLocateByTrigger(input)
        // Currency options have custom markup but Element Plus still uses the label for selection
        // The label format is "CODE - Name" (e.g., "EUR - Euro")
        popper.rootLocator
            .locator("xpath=//*[${XPath.hasClass("el-select-dropdown__item")}][normalize-space(.)='$currencyLabel']")
            .click()
        popper.shouldBeClosed()
    }

    fun shouldHaveSelectedValue(value: String) = select.shouldHaveSelectedValue(value)
    
    fun fill(text: String) {
        input.click()
        input.fill(text)
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
