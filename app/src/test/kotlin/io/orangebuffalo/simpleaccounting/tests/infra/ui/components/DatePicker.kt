package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveValue

class DatePicker private constructor(
    private val rootLocator: Locator,
) : UiComponent<DatePicker>() {
    private val inputLocator = rootLocator.locator("input")

    fun fill(date: String) {
        inputLocator.fill(date)
        inputLocator.blur()
    }

    fun shouldBeVisible() = inputLocator.shouldBeVisible()

    fun shouldHaveValue(value: String) = inputLocator.shouldHaveValue(value)

    fun shouldBeEnabled() = inputLocator.shouldBeEnabled()

    fun shouldBeDisabled() = inputLocator.shouldBeDisabled()

    fun clickDay(day: Int) {
        inputLocator.click()
        val popover = inputLocator.page().locator(".el-picker-panel__body-wrapper")
        popover.shouldBeVisible()
        val dayCell = popover.locator("td[class*='available'] .el-date-table-cell__text")
            .locator("text=${day}")
            .first()
        dayCell.click()
    }

    companion object {
        fun byContainer(container: Locator) = DatePicker(container)
    }
}
