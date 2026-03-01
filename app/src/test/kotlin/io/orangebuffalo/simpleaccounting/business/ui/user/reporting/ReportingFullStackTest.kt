package io.orangebuffalo.simpleaccounting.business.ui.user.reporting

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.reporting.ReportingPage.Companion.openReportingPage
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReportingFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should generate general tax report with all combinations of income and expense taxes`(page: Page) {
        page.authenticateViaCookie(preconditionsAllCombinations.fry)

        page.openReportingPage {
            selectGeneralTaxReport()
            steps {
                shouldHaveStepDescriptions("Tax Report", "Please select reporting date range", "")
            }

            dateRangePicker {
                fillDateRange("3025-01-01", "3025-12-31")
            }
            nextButton.click()

            steps {
                shouldHaveStepDescriptions("Tax Report", "3025-01-01 to 3025-12-31", "Ready")
            }

            // Collected section = from incomes
            collectedSection {
                shouldBeVisible()
                shouldHaveTableData(
                    TaxReportRow(
                        taxName = "Interplanetary VAT",
                        numberOfItems = "2",
                        itemsAmount = "USD 500.00",
                        taxAmount = "USD 100.00",
                    ),
                    TaxReportRow(
                        taxName = "Interplanetary VAT",
                        numberOfItems = "1",
                        itemsAmount = "",
                        taxAmount = "",
                    ),
                )
                shouldHaveTotal("USD 100.00")
            }

            // Paid section = from expenses
            paidSection {
                shouldBeVisible()
                shouldHaveTableData(
                    TaxReportRow(
                        taxName = "Interplanetary VAT",
                        numberOfItems = "1",
                        itemsAmount = "USD 200.00",
                        taxAmount = "USD 40.00",
                    ),
                    TaxReportRow(
                        taxName = "Interplanetary VAT",
                        numberOfItems = "1",
                        itemsAmount = "",
                        taxAmount = "",
                    ),
                )
                shouldHaveTotal("USD 40.00")
            }

            reportRendering("reporting.all-combinations")
        }
    }

    @Test
    fun `should generate general tax report with only finalized incomes`(page: Page) {
        page.authenticateViaCookie(preconditionsOnlyFinalizedIncomes.fry)

        page.openReportingPage {
            selectGeneralTaxReport()
            dateRangePicker {
                fillDateRange("3025-01-01", "3025-12-31")
            }
            nextButton.click()

            collectedSection {
                shouldBeVisible()
                shouldHaveTableData(
                    TaxReportRow(
                        taxName = "Moon Sales Tax",
                        numberOfItems = "1",
                        itemsAmount = "USD 1,000.00",
                        taxAmount = "USD 150.00",
                    ),
                )
                shouldHaveTotal("USD 150.00")
            }

            paidSection {
                shouldBeVisible()
                shouldHaveEmptyTable()
                shouldHaveTotal("USD 0.00")
            }

            reportRendering("reporting.only-finalized-incomes")
        }
    }

    @Test
    fun `should generate general tax report with only finalized expenses`(page: Page) {
        page.authenticateViaCookie(preconditionsOnlyFinalizedExpenses.fry)

        page.openReportingPage {
            selectGeneralTaxReport()
            dateRangePicker {
                fillDateRange("3025-01-01", "3025-12-31")
            }
            nextButton.click()

            collectedSection {
                shouldBeVisible()
                shouldHaveEmptyTable()
                shouldHaveTotal("USD 0.00")
            }

            paidSection {
                shouldBeVisible()
                shouldHaveTableData(
                    TaxReportRow(
                        taxName = "Moon Sales Tax",
                        numberOfItems = "1",
                        itemsAmount = "USD 300.00",
                        taxAmount = "USD 45.00",
                    ),
                )
                shouldHaveTotal("USD 45.00")
            }

            reportRendering("reporting.only-finalized-expenses")
        }
    }

    @Test
    fun `should generate general tax report with only pending items`(page: Page) {
        page.authenticateViaCookie(preconditionsOnlyPending.fry)

        page.openReportingPage {
            selectGeneralTaxReport()
            dateRangePicker {
                fillDateRange("3025-01-01", "3025-12-31")
            }
            nextButton.click()

            collectedSection {
                shouldBeVisible()
                shouldHaveTableData(
                    TaxReportRow(
                        taxName = "Robot Oil Tax",
                        numberOfItems = "1",
                        itemsAmount = "",
                        taxAmount = "",
                    ),
                )
                shouldHaveTotal("USD 0.00")
            }

            paidSection {
                shouldBeVisible()
                shouldHaveTableData(
                    TaxReportRow(
                        taxName = "Robot Oil Tax",
                        numberOfItems = "1",
                        itemsAmount = "",
                        taxAmount = "",
                    ),
                )
                shouldHaveTotal("USD 0.00")
            }

            reportRendering("reporting.only-pending")
        }
    }

    private val preconditionsAllCombinations by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val tax = generalTax(
                workspace = workspace,
                title = "Interplanetary VAT",
                rateInBps = 20_00,
            )

            init {
                // Finalized income with tax (collected, finalized)
                income(
                    workspace = workspace,
                    title = "Slurm delivery revenue",
                    dateReceived = LocalDate.of(3025, 3, 15),
                    currency = "USD",
                    originalAmount = 30000,
                    convertedAmounts = amountsInDefaultCurrency(30000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(30000),
                    generalTax = tax,
                    generalTaxRateInBps = 20_00,
                    generalTaxAmount = 6000,
                    status = IncomeStatus.FINALIZED,
                )
                // Another finalized income with tax (collected, finalized)
                income(
                    workspace = workspace,
                    title = "Robot repair income",
                    dateReceived = LocalDate.of(3025, 6, 20),
                    currency = "USD",
                    originalAmount = 20000,
                    convertedAmounts = amountsInDefaultCurrency(20000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(20000),
                    generalTax = tax,
                    generalTaxRateInBps = 20_00,
                    generalTaxAmount = 4000,
                    status = IncomeStatus.FINALIZED,
                )
                // Pending income with tax (collected, pending)
                income(
                    workspace = workspace,
                    title = "Moon cargo delivery",
                    dateReceived = LocalDate.of(3025, 9, 10),
                    currency = "USD",
                    originalAmount = 15000,
                    generalTax = tax,
                    generalTaxRateInBps = 20_00,
                    status = IncomeStatus.PENDING_CONVERSION,
                )
                // Finalized expense with tax (paid, finalized)
                expense(
                    workspace = workspace,
                    title = "Spaceship fuel",
                    datePaid = LocalDate.of(3025, 4, 5),
                    currency = "USD",
                    originalAmount = 20000,
                    convertedAmounts = amountsInDefaultCurrency(20000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(20000),
                    generalTax = tax,
                    generalTaxRateInBps = 20_00,
                    generalTaxAmount = 4000,
                    status = ExpenseStatus.FINALIZED,
                )
                // Pending expense with tax (paid, pending)
                expense(
                    workspace = workspace,
                    title = "Dark matter purchase",
                    datePaid = LocalDate.of(3025, 7, 12),
                    currency = "USD",
                    originalAmount = 10000,
                    generalTax = tax,
                    generalTaxRateInBps = 20_00,
                    status = ExpenseStatus.PENDING_CONVERSION,
                )
                // Income without tax (should not appear in report)
                income(
                    workspace = workspace,
                    title = "Tip from Nibbler",
                    dateReceived = LocalDate.of(3025, 5, 1),
                    currency = "USD",
                    originalAmount = 5000,
                    convertedAmounts = amountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(5000),
                    status = IncomeStatus.FINALIZED,
                )
                // Expense without tax (should not appear in report)
                expense(
                    workspace = workspace,
                    title = "Bender's beer fund",
                    datePaid = LocalDate.of(3025, 5, 15),
                    currency = "USD",
                    originalAmount = 3000,
                    convertedAmounts = amountsInDefaultCurrency(3000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(3000),
                    status = ExpenseStatus.FINALIZED,
                )
            }
        }
    }

    private val preconditionsOnlyFinalizedIncomes by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val tax = generalTax(
                workspace = workspace,
                title = "Moon Sales Tax",
                rateInBps = 15_00,
            )

            init {
                income(
                    workspace = workspace,
                    title = "Delivery to Omicron Persei 8",
                    dateReceived = LocalDate.of(3025, 2, 10),
                    currency = "USD",
                    originalAmount = 100000,
                    convertedAmounts = amountsInDefaultCurrency(100000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(100000),
                    generalTax = tax,
                    generalTaxRateInBps = 15_00,
                    generalTaxAmount = 15000,
                    status = IncomeStatus.FINALIZED,
                )
            }
        }
    }

    private val preconditionsOnlyFinalizedExpenses by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val tax = generalTax(
                workspace = workspace,
                title = "Moon Sales Tax",
                rateInBps = 15_00,
            )

            init {
                expense(
                    workspace = workspace,
                    title = "Spaceship maintenance",
                    datePaid = LocalDate.of(3025, 5, 20),
                    currency = "USD",
                    originalAmount = 30000,
                    convertedAmounts = amountsInDefaultCurrency(30000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(30000),
                    generalTax = tax,
                    generalTaxRateInBps = 15_00,
                    generalTaxAmount = 4500,
                    status = ExpenseStatus.FINALIZED,
                )
            }
        }
    }

    private val preconditionsOnlyPending by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val tax = generalTax(
                workspace = workspace,
                title = "Robot Oil Tax",
                rateInBps = 10_00,
            )

            init {
                // Pending income (collected, pending)
                income(
                    workspace = workspace,
                    title = "Pending Slurm payment",
                    dateReceived = LocalDate.of(3025, 4, 15),
                    currency = "USD",
                    originalAmount = 50000,
                    generalTax = tax,
                    generalTaxRateInBps = 10_00,
                    status = IncomeStatus.PENDING_CONVERSION,
                )
                // Pending expense (paid, pending)
                expense(
                    workspace = workspace,
                    title = "Pending dark matter delivery",
                    datePaid = LocalDate.of(3025, 8, 22),
                    currency = "USD",
                    originalAmount = 25000,
                    generalTax = tax,
                    generalTaxRateInBps = 10_00,
                    status = ExpenseStatus.PENDING_CONVERSION,
                )
            }
        }
    }
}
