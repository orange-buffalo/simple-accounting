package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveValue
import io.orangebuffalo.kotestplaywrightassertions.shouldContainText

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

    fun openPopover() {
        inputLocator.click()
        val popover = inputLocator.page().locator(".el-picker-panel__body-wrapper")
        popover.shouldBeVisible()
    }

    fun clickDay(day: Int) {
        inputLocator.click()
        val popover = inputLocator.page().locator(".el-picker-panel__body-wrapper")
        popover.shouldBeVisible()
        val dayCell = popover.locator("td[class*='available'] .el-date-table-cell__text")
            .locator("text=${day}")
            .first()
        dayCell.click()
    }

    fun shouldHavePopoverMonthYear(monthYear: String) {
        val popover = inputLocator.page().locator(".el-picker-panel__body-wrapper")
        val header = popover.locator(".el-date-picker__header")
        header.shouldContainText(monthYear)
    }

    fun shouldHavePopoverWeekday(weekday: String) {
        val popover = inputLocator.page().locator(".el-picker-panel__body-wrapper")
        popover.locator("th", Locator.LocatorOptions().setHasText(weekday)).first().shouldBeVisible()
    }

    companion object {
        fun byContainer(container: Locator) = DatePicker(container)
    }
}
