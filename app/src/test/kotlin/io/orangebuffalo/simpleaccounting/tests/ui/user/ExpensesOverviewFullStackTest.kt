package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DetailsSectionSpec
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Icons
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpenseOverviewItem
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.openExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.toExpenseOverviewItem
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExpensesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display expenses with all possible states and attributes`(page: Page) {
        page.authenticateViaCookie(preconditionsAllStates.fry)
        
        // Capture loading state by blocking API response
        page.withBlockedApiResponse(
            "workspaces/${preconditionsAllStates.workspace.id!!}/expenses*",
            initiator = {
                page.openExpensesOverviewPage()
            },
            blockedRequestSpec = {
                page.shouldBeExpensesOverviewPage {
                    pageItems.shouldHaveLoadingIndicatorVisible()
                    pageItems.reportRendering("expenses-overview.loading")
                }
            }
        )
        
        page.openExpensesOverviewPage {
            
            pageItems {
                // Verify all expenses with their complete data
                shouldHaveExactItems(
                    ExpenseOverviewItem(
                        title = "Finalized USD",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = "Jan 15, 2025",
                        amount = "USD 100.00",
                        attributePreviewIcons = emptyList()
                    ),
                    ExpenseOverviewItem(
                        title = "Pending Conversion EUR",
                        status = "pending",
                        statusText = "Pending",
                        datePaid = "Jan 14, 2025",
                        amount = "EUR 50.00",
                        attributePreviewIcons = listOf(Icons.MULTI_CURRENCY)
                    ),
                    ExpenseOverviewItem(
                        title = "Pending Tax Conversion",
                        status = "pending",
                        statusText = "Pending",
                        datePaid = "Jan 13, 2025",
                        amount = "USD 40.00",
                        attributePreviewIcons = listOf(Icons.MULTI_CURRENCY)
                    ),
                    ExpenseOverviewItem(
                        title = "With Notes",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = "Jan 12, 2025",
                        amount = "USD 20.00",
                        attributePreviewIcons = listOf(Icons.NOTES)
                    ),
                    ExpenseOverviewItem(
                        title = "With Tax",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = "Jan 11, 2025",
                        amount = "USD 100.00",
                        attributePreviewIcons = listOf(Icons.TAX)
                    ),
                    ExpenseOverviewItem(
                        title = "With Attachments",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = "Jan 10, 2025",
                        amount = "USD 50.00",
                        attributePreviewIcons = listOf(Icons.ATTACHMENT)
                    ),
                    ExpenseOverviewItem(
                        title = "Foreign Currency Same Amounts",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = "Jan 9, 2025",
                        amount = "USD 60.00",
                        attributePreviewIcons = listOf(Icons.MULTI_CURRENCY)
                    ),
                    ExpenseOverviewItem(
                        title = "Foreign Currency Different Amounts",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = "Jan 8, 2025",
                        amount = "USD 8.50",
                        attributePreviewIcons = listOf(Icons.MULTI_CURRENCY)
                    ),
                    ExpenseOverviewItem(
                        title = "Partial Business",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = "Jan 7, 2025",
                        amount = "USD 40.00",
                        attributePreviewIcons = listOf(Icons.PERCENT)
                    ),
                    ExpenseOverviewItem(
                        title = "Multiple Icons",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = "Jan 6, 2025",
                        amount = "USD 160.00",
                        attributePreviewIcons = listOf(Icons.ATTACHMENT, Icons.MULTI_CURRENCY, Icons.NOTES, Icons.PERCENT, Icons.TAX)
                    )
                ) { it.toExpenseOverviewItem() }
                
                // Report rendering with all panels collapsed
                reportRendering("expenses-overview.loaded-collapsed")
                
                // Expand and verify details for each expense
                staticItems[0].shouldHaveDetails(
                    actions = listOf("Copy", "Edit"),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to "Finalized",
                        "Category" to "Delivery",
                        "Date Paid" to "Jan 15, 2025",
                        "Adjusted Amount for Tax Purposes" to "USD 100.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 100.00"
                    )
                )
                
                staticItems[1].shouldHaveDetails(
                    actions = listOf("Copy", "Edit"),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to "Pending conversion to USD",
                        "Category" to "Delivery",
                        "Date Paid" to "Jan 14, 2025",
                        "Adjusted Amount for Tax Purposes" to "not provided yet"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Currency" to "EUR",
                        "Original Amount" to "EUR 50.00"
                    ),
                    DetailsSectionSpec(
                        title = "Foreign Currency Conversion",
                        "Converted Amount (USD)" to "not provided yet",
                        "Different Exchange Rate" to "No",
                        "Income Tax Amount (USD)" to "not provided yet"
                    )
                )
                
                staticItems[3].shouldHaveDetails(
                    actions = listOf("Copy", "Edit"),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to "Finalized",
                        "Category" to "Delivery",
                        "Date Paid" to "Jan 12, 2025",
                        "Adjusted Amount for Tax Purposes" to "USD 20.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 20.00"
                    ),
                    DetailsSectionSpec(
                        title = "Notes"
                    )
                )
                
                staticItems[4].shouldHaveDetails(
                    actions = listOf("Copy", "Edit"),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to "Finalized",
                        "Category" to "Delivery",
                        "Date Paid" to "Jan 11, 2025",
                        "Adjusted Amount for Tax Purposes" to "USD 100.00",
                        "General Tax" to "VAT",
                        "General Tax Rate" to "20.00%",
                        "General Tax Amount" to "USD 20.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 100.00"
                    )
                )
                
                staticItems[5].shouldHaveDetails(
                    actions = listOf("Copy", "Edit"),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to "Finalized",
                        "Category" to "Delivery",
                        "Date Paid" to "Jan 10, 2025",
                        "Adjusted Amount for Tax Purposes" to "USD 50.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 50.00"
                    ),
                    DetailsSectionSpec(
                        title = "Attachments"
                    )
                )
                
                staticItems[7].shouldHaveDetails(
                    actions = listOf("Copy", "Edit"),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to "Finalized",
                        "Category" to "Delivery",
                        "Date Paid" to "Jan 8, 2025",
                        "Adjusted Amount for Tax Purposes" to "USD 8.50"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Currency" to "JPY",
                        "Original Amount" to "JPY 1,000.00"
                    ),
                    DetailsSectionSpec(
                        title = "Foreign Currency Conversion",
                        "Converted Amount (USD)" to "USD 9.00",
                        "Different Exchange Rate" to "Yes",
                        "Income Tax Amount (USD)" to "USD 8.50"
                    )
                )
                
                staticItems[8].shouldHaveDetails(
                    actions = listOf("Copy", "Edit"),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to "Finalized",
                        "Category" to "Delivery",
                        "Date Paid" to "Jan 7, 2025",
                        "Adjusted Amount for Tax Purposes" to "USD 40.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 40.00",
                        "Business Use" to "70%"
                    )
                )
                
                staticItems[9].shouldHaveDetails(
                    actions = listOf("Copy", "Edit"),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to "Finalized",
                        "Category" to "Delivery",
                        "Date Paid" to "Jan 6, 2025",
                        "Adjusted Amount for Tax Purposes" to "USD 160.00",
                        "General Tax" to "VAT",
                        "General Tax Rate" to "20.00%",
                        "General Tax Amount" to "USD 32.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Currency" to "CHF",
                        "Original Amount" to "CHF 150.00",
                        "Business Use" to "60%"
                    ),
                    DetailsSectionSpec(
                        title = "Foreign Currency Conversion",
                        "Converted Amount (USD)" to "USD 160.00",
                        "Different Exchange Rate" to "No",
                        "Income Tax Amount (USD)" to "USD 160.00"
                    ),
                    DetailsSectionSpec(
                        title = "Attachments"
                    ),
                    DetailsSectionSpec(
                        title = "Notes"
                    )
                )
                
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
            
            // Report rendering with all panels expanded
            reportRendering("expenses-overview.loaded-expanded")
        }
    }

    @Test
    fun `should support pagination`(page: Page) {
        page.authenticateViaCookie(preconditionsPagination.fry)
        val firstPageExpenses = arrayOf(
            "Expense 1", "Expense 10", "Expense 11", "Expense 12", "Expense 13",
            "Expense 14", "Expense 15", "Expense 2", "Expense 3", "Expense 4"
        )
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveExactItems(*firstPageExpenses) { it.title!! }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                }
                shouldHaveExactItems(
                    "Expense 5", "Expense 6", "Expense 7", "Expense 8", "Expense 9"
                ) { it.title!! }
                paginator {
                    previous()
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
                shouldHaveExactItems(*firstPageExpenses) { it.title!! }
            }
        }
    }

    @Test
    fun `should support filtering by free text search`(page: Page) {
        page.authenticateViaCookie(preconditionsFiltering.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldContainItems("Office", "Travel", "Meals") { it.title!! }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
            }
            
            // Filter by title
            filterInput { fill("office") }
            pageItems {
                shouldHaveExactItems("Office") { it.title!! }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
            
            // Filter by category name
            filterInput { fill("") }
            filterInput { fill("travel") }
            pageItems {
                shouldHaveExactItems("Travel") { it.title!! }
            }
            
            // Filter by notes
            filterInput { fill("") }
            filterInput { fill("urgent") }
            pageItems {
                shouldHaveExactItems("Meals") { it.title!! }
            }
        }
    }

    private val preconditionsAllStates by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)
            val generalTax = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
            val document1 = document(workspace = workspace, name = "Receipt 1")
            val document2 = document(workspace = workspace, name = "Receipt 2")

            init {
                // 1. Finalized expense in default currency (USD)
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Finalized USD",
                    datePaid = LocalDate.of(2025, 1, 15),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
                
                // 2. Pending conversion - foreign currency not yet converted
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Pending Conversion EUR",
                    datePaid = LocalDate.of(2025, 1, 14),
                    currency = "EUR",
                    originalAmount = 5000,
                    convertedAmounts = emptyAmountsInDefaultCurrency(),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = ExpenseStatus.PENDING_CONVERSION
                )
                
                // 3. Pending conversion for taxation purposes
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Pending Tax Conversion",
                    datePaid = LocalDate.of(2025, 1, 13),
                    currency = "GBP",
                    originalAmount = 3000,
                    convertedAmounts = amountsInDefaultCurrency(4000),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    useDifferentExchangeRateForIncomeTaxPurposes = true
                )
                
                // 4. With notes
                expense(
                    workspace = workspace,
                    category = category,
                    title = "With Notes",
                    datePaid = LocalDate.of(2025, 1, 12),
                    originalAmount = 2000,
                    convertedAmounts = amountsInDefaultCurrency(2000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(2000),
                    status = ExpenseStatus.FINALIZED,
                    notes = "Important expense notes"
                )
                
                // 5. With general tax
                expense(
                    workspace = workspace,
                    category = category,
                    title = "With Tax",
                    datePaid = LocalDate.of(2025, 1, 11),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED,
                    generalTax = generalTax,
                    generalTaxRateInBps = 2000,
                    generalTaxAmount = 2000
                )
                
                // 6. With attachments (multiple)
                expense(
                    workspace = workspace,
                    category = category,
                    title = "With Attachments",
                    datePaid = LocalDate.of(2025, 1, 10),
                    originalAmount = 5000,
                    convertedAmounts = amountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(5000),
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf(document1, document2)
                )
                
                // 7. Foreign currency with same amounts for reporting and taxation
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Foreign Currency Same Amounts",
                    datePaid = LocalDate.of(2025, 1, 9),
                    currency = "CAD",
                    originalAmount = 8000,
                    convertedAmounts = amountsInDefaultCurrency(6000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(6000),
                    status = ExpenseStatus.FINALIZED,
                    useDifferentExchangeRateForIncomeTaxPurposes = false
                )
                
                // 8. Foreign currency with different amounts for reporting and taxation
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Foreign Currency Different Amounts",
                    datePaid = LocalDate.of(2025, 1, 8),
                    currency = "JPY",
                    originalAmount = 100000,
                    convertedAmounts = amountsInDefaultCurrency(900),
                    incomeTaxableAmounts = amountsInDefaultCurrency(850),
                    status = ExpenseStatus.FINALIZED,
                    useDifferentExchangeRateForIncomeTaxPurposes = true
                )
                
                // 9. Partial business purpose
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Partial Business",
                    datePaid = LocalDate.of(2025, 1, 7),
                    originalAmount = 4000,
                    convertedAmounts = amountsInDefaultCurrency(4000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(4000),
                    status = ExpenseStatus.FINALIZED,
                    percentOnBusiness = 70
                )
                
                // 10. Multiple icons - expense with all attributes
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Multiple Icons",
                    datePaid = LocalDate.of(2025, 1, 6),
                    currency = "CHF",
                    originalAmount = 15000,
                    convertedAmounts = amountsInDefaultCurrency(16000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(16000),
                    status = ExpenseStatus.FINALIZED,
                    generalTax = generalTax,
                    generalTaxRateInBps = 2000,
                    generalTaxAmount = 3200,
                    attachments = setOf(document1),
                    percentOnBusiness = 60,
                    notes = "Complex expense with all attributes",
                    useDifferentExchangeRateForIncomeTaxPurposes = false
                )
            }
        }
    }

    private val preconditionsPagination by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)

            init {
                val baseDate = LocalDate.of(2025, 1, 1)
                (1..15).forEach { index ->
                    expense(
                        workspace = workspace,
                        category = category,
                        title = "Expense $index",
                        datePaid = baseDate.minusDays(index.toLong()),
                        originalAmount = 1000 * index.toLong(),
                        convertedAmounts = amountsInDefaultCurrency(1000 * index.toLong()),
                        incomeTaxableAmounts = amountsInDefaultCurrency(1000 * index.toLong()),
                        status = ExpenseStatus.FINALIZED
                    )
                }
            }
        }
    }

    private val preconditionsFiltering by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val officeCategory = category(workspace = workspace, name = "Office")
            val travelCategory = category(workspace = workspace, name = "Travel")
            val mealsCategory = category(workspace = workspace, name = "Meals")

            init {
                val baseDate = LocalDate.of(2025, 1, 1)
                expense(
                    workspace = workspace,
                    category = officeCategory,
                    title = "Office",
                    datePaid = baseDate.minusDays(1),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
                expense(
                    workspace = workspace,
                    category = travelCategory,
                    title = "Travel",
                    datePaid = baseDate.minusDays(2),
                    originalAmount = 5000,
                    convertedAmounts = amountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(5000),
                    status = ExpenseStatus.FINALIZED
                )
                expense(
                    workspace = workspace,
                    category = mealsCategory,
                    title = "Meals",
                    datePaid = baseDate.minusDays(3),
                    originalAmount = 3000,
                    convertedAmounts = amountsInDefaultCurrency(3000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(3000),
                    status = ExpenseStatus.FINALIZED,
                    notes = "This is urgent"
                )
                (1..10).forEach { index ->
                    expense(
                        workspace = workspace,
                        category = officeCategory,
                        title = "Other $index",
                        datePaid = baseDate.minusDays(10L + index),
                        originalAmount = 1000,
                        convertedAmounts = amountsInDefaultCurrency(1000),
                        incomeTaxableAmounts = amountsInDefaultCurrency(1000),
                        status = ExpenseStatus.FINALIZED
                    )
                }
            }
        }
    }
}
