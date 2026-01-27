package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator

class CurrencyInput private constructor(
    private val rootLocator: Locator,
) : UiComponent<CurrencyInput>() {
    private val select = Select.byContainer(rootLocator)

    fun selectOption(option: String) = select.selectOption(option)

    fun shouldHaveSelectedValue(value: String) = select.shouldHaveSelectedValue(value)

    fun shouldBeVisible() = select.shouldBeVisible()

    companion object {
        fun byContainer(container: Locator) = CurrencyInput(container)
    }
}
