package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveValue

class MoneyInput private constructor(
    private val rootLocator: Locator,
) : UiComponent<MoneyInput>() {
    private val inputLocator = rootLocator.locator(".sa-money-input__input")
    private val currencyLocator = rootLocator.locator(".sa-money-input__currency")

    fun fill(value: String) {
        inputLocator.waitFor()
        inputLocator.fill(value)
        // Trigger blur to ensure IMask processes the value
        inputLocator.blur()
    }

    fun shouldBeVisible() = inputLocator.shouldBeVisible()

    fun shouldHaveValue(value: String) = inputLocator.shouldHaveValue(value)

    fun shouldHaveCurrency(currency: String) {
        currencyLocator.shouldBeVisible()
        currencyLocator.shouldHaveText(currency)
    }

    fun shouldBeEnabled() = inputLocator.shouldBeEnabled()

    fun shouldBeDisabled() = inputLocator.shouldBeDisabled()

    companion object {
        fun byContainer(container: Locator) = MoneyInput(container)
    }
}
