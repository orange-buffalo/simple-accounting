package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveValue

class InputNumber private constructor(
    private val rootLocator: Locator,
) : UiComponent<InputNumber>() {
    private val inputLocator = rootLocator.locator("input")

    fun fill(value: String) = inputLocator.fill(value)

    fun shouldBeVisible() = inputLocator.shouldBeVisible()

    fun shouldHaveValue(value: String) = inputLocator.shouldHaveValue(value)

    fun shouldBeEnabled() = inputLocator.shouldBeEnabled()

    fun shouldBeDisabled() = inputLocator.shouldBeDisabled()

    companion object {
        fun byContainer(container: Locator) = InputNumber(container.locator(".el-input-number"))
    }
}
