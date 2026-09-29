package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@UiComponentMarker
class ReportingInlineCalendar private constructor(panel: Locator) : UiComponent<ReportingInlineCalendar>() {
    private val calendar = panel.locator(".reporting-panel--calendar")
    private val selectedRange = panel.locator(".reporting-panel--selected-range")
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH)
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH)

    fun shouldBeVisible() {
        calendar.shouldBeVisible()
    }

    fun shouldHaveSelectedRange(start: LocalDate, end: LocalDate) {
        selectedRange.shouldHaveText("${start.format(dateFormatter)}—${end.format(dateFormatter)}")
    }

    fun selectRange(start: LocalDate, end: LocalDate) {
        val header = calendar.locator(".el-date-range-picker__content.is-left .el-date-range-picker__header-label")
        val currentMonth = YearMonth.parse(
            "${header.nth(1).innerText()} ${header.nth(0).innerText().trim()}", monthFormatter
        )
        val monthsToStart = ChronoUnit.MONTHS.between(currentMonth, YearMonth.from(start)).toInt()
        val navigation = if (monthsToStart < 0) "Previous Month" else "Next Month"
        repeat(kotlin.math.abs(monthsToStart)) {
            calendar.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName(navigation)).click()
        }
        selectDay(0, start.dayOfMonth)
        val monthsToEnd = ChronoUnit.MONTHS.between(YearMonth.from(start).plusMonths(1), YearMonth.from(end)).toInt()
        repeat(monthsToEnd) {
            calendar.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Next Month")).click()
        }
        selectDay(1, end.dayOfMonth)
    }

    fun selectDay(monthIndex: Int, day: Int) {
        calendar.locator(".el-date-range-picker__content").nth(monthIndex)
            .locator("td.available .el-date-table-cell__text")
            .getByText(day.toString(), Locator.GetByTextOptions().setExact(true))
            .click()
    }

    companion object {
        fun ComponentsAccessors.reportingInlineCalendarByContainer(container: Locator) =
            ReportingInlineCalendar(container)
    }
}
