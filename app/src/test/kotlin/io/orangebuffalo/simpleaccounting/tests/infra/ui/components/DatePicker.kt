package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible

class DatePicker private constructor(
    private val rootLocator: Locator,
) : UiComponent<DatePicker>() {
    private val inputLocator = rootLocator.locator("input")

    fun fill(date: String) {
        inputLocator.fill(date)
    }

    fun shouldBeVisible() = inputLocator.shouldBeVisible()

    fun shouldBeEnabled() = inputLocator.shouldBeEnabled()

    fun shouldBeDisabled() = inputLocator.shouldBeDisabled()

    companion object {
        fun byContainer(container: Locator) = DatePicker(container)
    }
}
