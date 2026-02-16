package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.DashboardPage.Companion.openDashboard
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.DashboardPage.Companion.shouldBeDashboardPage
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The fixed date used in tests, matching the browser's mocked time.
 * This ensures test data and browser date filters are always in sync.
 */
private val testFixedDate: LocalDate = MOCK_TIME
    .atZone(ZoneOffset.UTC)
    .toLocalDate()

class DashboardFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should use default dates when localStorage is empty`(page: Page) {
        page.authenticateViaCookie(preconditionsEmpty.fry)
        val today = testFixedDate
        val startOfYear = LocalDate.of(today.year, 1, 1)
        
        page.openDashboard {
            dateRangePicker {
                shouldHaveDateRange(startOfYear, today)
            }
            expensesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
                shouldHaveFinalizedText("Total of 0 expenses")
            }
            incomesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
                shouldHaveFinalizedText("Total of 0 incomes")
            }
            profitCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
            }
        }
    }

    @Test
    fun `should display empty state when no data exists`(page: Page) {
        page.authenticateViaCookie(preconditionsEmpty.fry)
        page.openDashboard {
            expensesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
                shouldHaveFinalizedText("Total of 0 expenses")
                shouldHaveNoPendingText()
                shouldHaveDetailsItemsCount(0)
            }
            
            incomesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
                shouldHaveFinalizedText("Total of 0 incomes")
                shouldHaveNoPendingText()
                shouldHaveDetailsItemsCount(0)
            }
            
            profitCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
                shouldHaveDetailsItemsCount(3)
            }
            
            shouldHaveInvoiceCards(0)
            
            reportRendering("dashboard.empty-state")
        }
    }

    @Test
    fun `should display expenses and incomes without pending items`(page: Page) {
        page.authenticateViaCookie(preconditionsWithFinalized.fry)
        page.openDashboard {
            expensesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 120.00")
                shouldHaveFinalizedText("Total of 2 expenses")
                shouldHaveNoPendingText()
                shouldHaveDetailsItemsCount(2)
                shouldHaveDetailsItem(0, "Office", "USD 100.00")
                shouldHaveDetailsItem(1, "Travel", "USD 20.00")
            }
            
            incomesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 250.00")
                shouldHaveFinalizedText("Total of 2 incomes")
                shouldHaveNoPendingText()
                shouldHaveDetailsItemsCount(2)
                shouldHaveDetailsItem(0, "Consulting", "USD 200.00")
                shouldHaveDetailsItem(1, "Sales", "USD 50.00")
            }
            
            profitCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 130.00")
                shouldHaveDetailsItemsCount(3)
                shouldHaveDetailsItem(0, "Income Tax Payments", "USD 0.00")
                shouldHaveDetailsItem(1, "Estimated Tax", "coming soon..")
                shouldHaveDetailsItem(2, "Profit", "USD 130.00")
            }
            
            shouldHaveInvoiceCards(0)
        }
    }

    @Test
    fun `should display expenses and incomes with pending items`(page: Page) {
        page.authenticateViaCookie(preconditionsWithPending.fry)
        page.openDashboard {
            expensesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 120.00")
                shouldHaveFinalizedText("Total of 2 expenses")
                shouldHavePendingText("Pending 1 more")
                shouldHaveDetailsItemsCount(2)
                shouldHaveDetailsItem(0, "Office", "USD 100.00")
                shouldHaveDetailsItem(1, "Travel", "USD 20.00")
            }
            
            incomesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 250.00")
                shouldHaveFinalizedText("Total of 2 incomes")
                shouldHavePendingText("Pending 2 more")
                shouldHaveDetailsItemsCount(2)
                shouldHaveDetailsItem(0, "Consulting", "USD 200.00")
                shouldHaveDetailsItem(1, "Sales", "USD 50.00")
            }
            
            profitCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 130.00")
                shouldHaveDetailsItemsCount(3)
            }
            
            shouldHaveInvoiceCards(0)
        }
    }

    @Test
    fun `should display invoices with SENT and OVERDUE status`(page: Page) {
        page.authenticateViaCookie(preconditionsWithInvoices.fry)
        page.openDashboard {
            expensesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
                shouldHaveFinalizedText("Total of 0 expenses")
            }
            
            incomesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
                shouldHaveFinalizedText("Total of 0 incomes")
            }
            
            profitCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
            }
            
            shouldHaveInvoiceCards(2)
            
            invoiceCard(0) {
                shouldHaveTitle("Invoice 1")
                shouldHaveAmount("USD 100.00")
                shouldHaveStatus("Pending")
                shouldHaveDetailsItem(0, "To", "Customer A")
            }
            
            invoiceCard(1) {
                shouldHaveTitle("Invoice 2")
                shouldHaveAmount("USD 150.00")
                shouldHaveStatus("Overdue")
                shouldHaveDetailsItem(0, "To", "Customer B")
            }
            
            reportRendering("dashboard.with-invoices")
        }
    }

    @Test
    fun `should not display invoices with other statuses`(page: Page) {
        page.authenticateViaCookie(preconditionsWithOtherInvoices.fry)
        page.openDashboard {
            expensesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
                shouldHaveFinalizedText("Total of 0 expenses")
            }
            
            incomesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
                shouldHaveFinalizedText("Total of 0 incomes")
            }
            
            profitCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 0.00")
            }
            
            shouldHaveInvoiceCards(0)
            
            reportRendering("dashboard.no-invoices-state")
        }
    }

    @Test
    fun `should display loading state during API calls`(page: Page) {
        page.authenticateViaCookie(preconditionsWithFinalized.fry)
        
        page.withBlockedApiResponse(
            "workspaces/${preconditionsWithFinalized.workspace.id!!}/statistics/expenses**",
            initiator = {
                page.openDashboard {
                    // Ensure the expenses card is rendered to guarantee the API request has been initiated
                    expensesCard {
                        shouldBePresent()
                    }
                }
            },
            blockedRequestSpec = {
                page.shouldBeDashboardPage {
                    expensesCard {
                        shouldBeLoading()
                    }
                    reportRendering("dashboard.loading-state")
                }
            },
            resetOnCompletion = false
        )
        
        // Unroute to allow subsequent requests to complete
        page.context().unroute("/api/workspaces/${preconditionsWithFinalized.workspace.id!!}/statistics/expenses**")
        
        page.shouldBeDashboardPage {
            expensesCard {
                shouldBeLoaded()
            }
            incomesCard {
                shouldBeLoaded()
            }
            profitCard {
                shouldBeLoaded()
            }
        }
    }

    @Test
    fun `should filter data by selected date range`(page: Page) {
        page.authenticateViaCookie(preconditionsForDateFiltering.fry)
        
        val today = testFixedDate
        val startOfYear = LocalDate.of(today.year, 1, 1)
        
        page.openDashboard {
            dateRangePicker {
                shouldHaveDateRange(startOfYear, today)
            }
            
            expensesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 300.00")
                shouldHaveFinalizedText("Total of 3 expenses")
            }
            
            incomesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 600.00")
                shouldHaveFinalizedText("Total of 3 incomes")
            }
        }
    }

    private val preconditionsEmpty by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }

    private val preconditionsWithFinalized by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val officeCategory = category(workspace = workspace, name = "Office")
            val travelCategory = category(workspace = workspace, name = "Travel")
            val consultingCategory = category(workspace = workspace, name = "Consulting")
            val salesCategory = category(workspace = workspace, name = "Sales")
            
            init {
                val today = testFixedDate
                val startOfYear = LocalDate.of(today.year, 1, 1)
                
                expense(
                    workspace = workspace,
                    category = officeCategory,
                    datePaid = startOfYear.plusDays(10),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
                expense(
                    workspace = workspace,
                    category = travelCategory,
                    datePaid = startOfYear.plusDays(20),
                    originalAmount = 2000,
                    convertedAmounts = amountsInDefaultCurrency(2000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(2000),
                    status = ExpenseStatus.FINALIZED
                )
                
                income(
                    workspace = workspace,
                    category = consultingCategory,
                    dateReceived = startOfYear.plusDays(15),
                    originalAmount = 20000,
                    convertedAmounts = amountsInDefaultCurrency(20000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(20000),
                    status = IncomeStatus.FINALIZED
                )
                income(
                    workspace = workspace,
                    category = salesCategory,
                    dateReceived = startOfYear.plusDays(25),
                    originalAmount = 5000,
                    convertedAmounts = amountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(5000),
                    status = IncomeStatus.FINALIZED
                )
            }
        }
    }

    private val preconditionsWithPending by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val officeCategory = category(workspace = workspace, name = "Office")
            val travelCategory = category(workspace = workspace, name = "Travel")
            val consultingCategory = category(workspace = workspace, name = "Consulting")
            val salesCategory = category(workspace = workspace, name = "Sales")
            
            init {
                val today = testFixedDate
                val startOfYear = LocalDate.of(today.year, 1, 1)
                
                expense(
                    workspace = workspace,
                    category = officeCategory,
                    datePaid = startOfYear.plusDays(10),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
                expense(
                    workspace = workspace,
                    category = travelCategory,
                    datePaid = startOfYear.plusDays(20),
                    originalAmount = 2000,
                    convertedAmounts = amountsInDefaultCurrency(2000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(2000),
                    status = ExpenseStatus.FINALIZED
                )
                expense(
                    workspace = workspace,
                    category = travelCategory,
                    datePaid = startOfYear.plusDays(30),
                    originalAmount = 1500,
                    convertedAmounts = amountsInDefaultCurrency(1500),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
                )
                
                income(
                    workspace = workspace,
                    category = consultingCategory,
                    dateReceived = startOfYear.plusDays(15),
                    originalAmount = 20000,
                    convertedAmounts = amountsInDefaultCurrency(20000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(20000),
                    status = IncomeStatus.FINALIZED
                )
                income(
                    workspace = workspace,
                    category = salesCategory,
                    dateReceived = startOfYear.plusDays(25),
                    originalAmount = 5000,
                    convertedAmounts = amountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(5000),
                    status = IncomeStatus.FINALIZED
                )
                income(
                    workspace = workspace,
                    category = salesCategory,
                    dateReceived = startOfYear.plusDays(30),
                    originalAmount = 3000,
                    convertedAmounts = emptyAmountsInDefaultCurrency(),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = IncomeStatus.PENDING_CONVERSION
                )
                income(
                    workspace = workspace,
                    category = consultingCategory,
                    dateReceived = startOfYear.plusDays(35),
                    originalAmount = 4000,
                    convertedAmounts = amountsInDefaultCurrency(4000),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    useDifferentExchangeRateForIncomeTaxPurposes = true
                )
            }
        }
    }

    private val preconditionsWithInvoices by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val customer1 = customer(workspace = workspace, name = "Customer A")
            val customer2 = customer(workspace = workspace, name = "Customer B")
            
            init {
                val today = testFixedDate
                val startOfYear = LocalDate.of(today.year, 1, 1)
                
                invoice(
                    customer = customer1,
                    title = "Invoice 1",
                    dateIssued = startOfYear.plusDays(10),
                    dateSent = startOfYear.plusDays(11),
                    dueDate = startOfYear.plusDays(40),
                    amount = 10000,
                    status = InvoiceStatus.SENT
                )
                invoice(
                    customer = customer2,
                    title = "Invoice 2",
                    dateIssued = startOfYear.plusDays(5),
                    dateSent = startOfYear.plusDays(6),
                    dueDate = startOfYear.minusDays(1),
                    amount = 15000,
                    status = InvoiceStatus.OVERDUE
                )
            }
        }
    }

    private val preconditionsWithOtherInvoices by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val customer = customer(workspace = workspace, name = "Customer C")
            
            init {
                val today = testFixedDate
                val startOfYear = LocalDate.of(today.year, 1, 1)
                
                invoice(
                    customer = customer,
                    title = "Draft Invoice",
                    dateIssued = startOfYear.plusDays(10),
                    dueDate = startOfYear.plusDays(40),
                    amount = 10000,
                    status = InvoiceStatus.DRAFT
                )
                invoice(
                    customer = customer,
                    title = "Paid Invoice",
                    dateIssued = startOfYear.plusDays(5),
                    dateSent = startOfYear.plusDays(6),
                    datePaid = startOfYear.plusDays(20),
                    dueDate = startOfYear.plusDays(30),
                    amount = 15000,
                    status = InvoiceStatus.PAID
                )
                invoice(
                    customer = customer,
                    title = "Cancelled Invoice",
                    dateIssued = startOfYear.plusDays(15),
                    dateSent = startOfYear.plusDays(16),
                    dueDate = startOfYear.plusDays(45),
                    amount = 20000,
                    status = InvoiceStatus.CANCELLED
                )
            }
        }
    }

    private val preconditionsForDateFiltering by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)
            
            init {
                val today = testFixedDate
                val startOfYear = LocalDate.of(today.year, 1, 1)
                
                // Expense in early January (before today)
                expense(
                    workspace = workspace,
                    category = category,
                    datePaid = startOfYear.plusDays(10),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
                // Expense in late January (before today)
                expense(
                    workspace = workspace,
                    category = category,
                    datePaid = startOfYear.plusDays(30),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
                // Expense in February (before today)
                expense(
                    workspace = workspace,
                    category = category,
                    datePaid = startOfYear.plusDays(50),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
                
                // Income in early January (before today)
                income(
                    workspace = workspace,
                    category = category,
                    dateReceived = startOfYear.plusDays(10),
                    originalAmount = 20000,
                    convertedAmounts = amountsInDefaultCurrency(20000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(20000),
                    status = IncomeStatus.FINALIZED
                )
                // Income in late January (before today)
                income(
                    workspace = workspace,
                    category = category,
                    dateReceived = startOfYear.plusDays(30),
                    originalAmount = 20000,
                    convertedAmounts = amountsInDefaultCurrency(20000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(20000),
                    status = IncomeStatus.FINALIZED
                )
                // Income in February (before today)
                income(
                    workspace = workspace,
                    category = category,
                    dateReceived = startOfYear.plusDays(50),
                    originalAmount = 20000,
                    convertedAmounts = amountsInDefaultCurrency(20000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(20000),
                    status = IncomeStatus.FINALIZED
                )
            }
        }
    }
}
