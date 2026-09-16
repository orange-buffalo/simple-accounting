package io.orangebuffalo.simpleaccounting.business.ui.user.reporting

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldContainText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ComponentsAccessors
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DateRangePicker.Companion.dateRangePickerByContainer
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Steps.Companion.stepsByContainer
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponent
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

class ReportingPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Reporting")
    private val reportingPanel = page.locator(".reporting-panel")
    val nextButton = components.buttonByText("Next")
    val dateRangePicker = components.dateRangePickerByContainer(reportingPanel)
    val steps = components.stepsByContainer(reportingPanel)
    val collectedSection = TaxReportSection(components, "Collected", 0)
    val paidSection = TaxReportSection(components, "Paid", 1)
    val incomeTaxReport = IncomeTaxReportComponent(components)

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun selectGeneralTaxReport() {
        reportingPanel.getByRole(AriaRole.HEADING, Locator.GetByRoleOptions().setName("General Tax Report"))
            .locator("..").click()
    }

    fun selectIncomeTaxReport() {
        reportingPanel.getByRole(AriaRole.HEADING, Locator.GetByRoleOptions().setName("Income Tax Report"))
            .locator("..").click()
    }

    companion object {
        fun Page.shouldBeReportingPage(spec: ReportingPage.() -> Unit = {}) {
            ReportingPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openReportingPage(spec: ReportingPage.() -> Unit = {}) {
            navigate("/reporting")
            shouldBeReportingPage(spec)
        }
    }
}

class TaxReportSection(
    components: ComponentsAccessors,
    title: String,
    private val sectionIndex: Int,
) : UiComponent<TaxReportSection>() {
    private val sectionHeader = components.sectionHeader(title)
    private val page = components.page
    private val totalLocator = page.locator(".reporting-panel--content b").nth(sectionIndex)

    fun shouldBeVisible() {
        sectionHeader.shouldBeVisible()
    }

    fun shouldHaveTotal(total: String) {
        totalLocator.shouldSatisfy {
            shouldContainText(total)
        }
    }

    fun shouldHaveTableData(vararg expectedRows: TaxReportRow) {
        shouldSatisfy("Table should contain expected rows") {
            @Suppress("UNCHECKED_CAST")
            val rows = page.evaluate(
                """(tableIndex) => {
                    const tables = document.querySelectorAll('.reporting-panel--content .el-table');
                    if (!tables[tableIndex]) return [];
                    const rows = tables[tableIndex].querySelectorAll('tbody tr.el-table__row');
                    return Array.from(rows).map(row => {
                        const cells = row.querySelectorAll('td .cell');
                        return [
                            (cells[0]?.textContent || '').replace(/\u00A0/g, ' ').trim(),
                            (cells[1]?.textContent || '').replace(/\u00A0/g, ' ').trim(),
                            (cells[2]?.textContent || '').replace(/\u00A0/g, ' ').trim(),
                            (cells[3]?.textContent || '').replace(/\u00A0/g, ' ').trim()
                        ];
                    });
                }""",
                sectionIndex
            ) as List<List<String>>
            val actualRows = rows.map { cells ->
                TaxReportRow(
                    taxName = cells[0],
                    numberOfItems = cells[1],
                    itemsAmount = cells[2],
                    taxAmount = cells[3],
                )
            }
            actualRows.shouldContainExactly(*expectedRows)
        }
    }

    fun shouldHaveEmptyTable() {
        page.locator(".reporting-panel--content .el-table").nth(sectionIndex)
            .locator(".el-table__empty-text").shouldBeVisible()
    }
}

data class TaxReportRow(
    val taxName: String,
    val numberOfItems: String,
    val itemsAmount: String,
    val taxAmount: String,
)

class IncomeTaxReportComponent(
    components: ComponentsAccessors,
) : UiComponent<IncomeTaxReportComponent>() {
    private val report = components.page.locator(".income-tax-report")
    private val netTaxableIncome = report.locator(".income-tax-report__net")
    val incomes = IncomeTaxReportSection(components, report, "Incomes", 0)
    val expenses = IncomeTaxReportSection(components, report, "Expenses", 1)

    fun shouldHaveNetTaxableIncome(amount: String) {
        netTaxableIncome.shouldSatisfy {
            shouldContainText(amount)
        }
    }
}

class IncomeTaxReportSection(
    components: ComponentsAccessors,
    report: Locator,
    title: String,
    sectionIndex: Int,
) : UiComponent<IncomeTaxReportSection>() {
    private val sectionHeader = components.sectionHeader(title)
    private val table = report.locator(".el-table").nth(sectionIndex)
    private val total = report.locator(".income-tax-report__total").nth(sectionIndex)

    fun shouldBeVisible() {
        sectionHeader.shouldBeVisible()
    }

    fun shouldHaveTotal(expected: String) {
        total.shouldSatisfy { shouldContainText(expected) }
    }

    fun shouldHaveTableData(vararg expectedRows: IncomeTaxReportRow) {
        shouldSatisfy("Table should contain expected rows") {
            @Suppress("UNCHECKED_CAST")
            val rows = table.evaluate(
                """table => Array.from(table.querySelectorAll('tbody tr.el-table__row')).map(row =>
                    Array.from(row.querySelectorAll('td .cell')).map(cell =>
                        (cell.textContent || '').replace(/\u00A0/g, ' ').trim()
                    )
                )"""
            ) as List<List<String>>
            rows.map { cells ->
                IncomeTaxReportRow(
                    categoryName = cells[0],
                    numberOfItems = cells[1],
                    amount = cells[2],
                )
            }.shouldContainExactly(*expectedRows)
        }
    }
}

data class IncomeTaxReportRow(
    val categoryName: String,
    val numberOfItems: String,
    val amount: String,
)
