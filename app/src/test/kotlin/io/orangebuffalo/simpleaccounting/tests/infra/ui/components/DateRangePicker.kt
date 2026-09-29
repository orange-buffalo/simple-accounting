package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@UiComponentMarker
class DateRangePicker private constructor(
    private val picker: Locator,
) : UiComponent<DateRangePicker>() {
    private val startInput = picker.locator("input").first()
    private val endInput = picker.locator("input").nth(1)
    
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")

    fun shouldHaveDateRange(startDate: LocalDate, endDate: LocalDate) {
        val startValue = startInput.inputValue()
        val endValue = endInput.inputValue()
        
        val actualStart = LocalDate.parse(startValue, formatter)
        val actualEnd = LocalDate.parse(endValue, formatter)
        
        actualStart.shouldBe(startDate)
        actualEnd.shouldBe(endDate)
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
