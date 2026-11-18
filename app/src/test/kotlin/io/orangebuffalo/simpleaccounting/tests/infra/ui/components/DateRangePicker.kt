package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@UiComponentMarker
class DateRangePicker private constructor(
    private val picker: Locator,
) : UiComponent<DateRangePicker>() {
    private val startInput = picker.locator("input").first()
    private val endInput = picker.locator("input").nth(1)
    
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")

    fun shouldHaveDateRange(startDate: LocalDate, endDate: LocalDate, inputPattern: String = "dd/MM/uuuu") {
        val inputFormatter = DateTimeFormatter.ofPattern(inputPattern)
        startInput.shouldHaveValue(startDate.format(inputFormatter))
        endInput.shouldHaveValue(endDate.format(inputFormatter))
    }

    fun fillDateRange(startDate: String, endDate: String) {
        startInput.fill(LocalDate.parse(startDate).format(formatter))
        endInput.click()
        endInput.fill(LocalDate.parse(endDate).format(formatter))
        endInput.press("Enter")
    }

    companion object {
        fun ComponentsAccessors.dateRangePicker() =
            DateRangePicker(page.locator(".el-date-editor"))

        fun ComponentsAccessors.dateRangePickerByContainer(container: Locator) =
            DateRangePicker(container.locator(".el-date-editor"))
    }
}
