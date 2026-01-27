package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator

class CurrencyInput private constructor(
    private val rootLocator: Locator,
) : UiComponent<CurrencyInput>() {
    private val select = Select.byContainer(rootLocator)
    private val input = rootLocator.locator(".el-select__wrapper")

    /**
     * Selects a currency by its code (e.g., "EUR", "USD").
     * Works with the custom currency option template that displays code and name separately.
     */
    fun selectOption(currencyCode: String) {
        val popper = Popper.openOrLocateByTrigger(input)
        // Currency options have custom markup with separate spans for code and name
        // We locate by the currency code span
        popper.rootLocator
            .locator(".sa-currency-input__currency-code:has-text(\"$currencyCode\")")
            .first()
            .click()
        popper.shouldBeClosed()
    }

    fun shouldHaveSelectedValue(value: String) = select.shouldHaveSelectedValue(value)

    fun shouldBeVisible() = select.shouldBeVisible()

    fun shouldBeHidden() = select.shouldBeHidden()

    fun shouldBeDisabled() = select.shouldBeDisabled()

    fun shouldHaveGroupedOptions(spec: (actualOptions: List<OptionsGroup>) -> Unit) =
        select.shouldHaveGroupedOptions(spec)

    companion object {
        fun byContainer(container: Locator) = CurrencyInput(container)
    }
}
