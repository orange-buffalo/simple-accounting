package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpenseOverviewItem
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.openExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.toExpenseOverviewItem
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExpensesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display expenses with all possible states and attributes`(page: Page) {
        page.authenticateViaCookie(preconditionsAllStates.fry)
        page.openExpensesOverviewPage {
            pageItems {
                // Verify all expenses with their complete data
                shouldHaveExactItems(
                    ExpenseOverviewItem(
                        title = "Finalized USD",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = preconditionsAllStates.formattedToday1,
                        attributePreviewIcons = emptyList()
                    ),
                    ExpenseOverviewItem(
                        title = "Pending Conversion EUR",
                        status = "pending",
                        statusText = "Pending",
                        datePaid = preconditionsAllStates.formattedToday2,
                        attributePreviewIcons = listOf("multi-currency")
                    ),
                    ExpenseOverviewItem(
                        title = "Pending Tax Conversion",
                        status = "pending",
                        statusText = "Pending",
                        datePaid = preconditionsAllStates.formattedToday3,
                        attributePreviewIcons = listOf("multi-currency")
                    ),
                    ExpenseOverviewItem(
                        title = "With Notes",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = preconditionsAllStates.formattedToday4,
                        attributePreviewIcons = listOf("notes")
                    ),
                    ExpenseOverviewItem(
                        title = "With Tax",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = preconditionsAllStates.formattedToday5,
                        attributePreviewIcons = listOf("tax")
                    ),
                    ExpenseOverviewItem(
                        title = "With Attachments",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = preconditionsAllStates.formattedToday6,
                        attributePreviewIcons = listOf("attachment")
                    ),
                    ExpenseOverviewItem(
                        title = "Foreign Currency Same Amounts",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = preconditionsAllStates.formattedToday7,
                        attributePreviewIcons = listOf("multi-currency")
                    ),
                    ExpenseOverviewItem(
                        title = "Foreign Currency Different Amounts",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = preconditionsAllStates.formattedToday8,
                        attributePreviewIcons = listOf("multi-currency")
                    ),
                    ExpenseOverviewItem(
                        title = "Partial Business",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = preconditionsAllStates.formattedToday9,
                        attributePreviewIcons = listOf("percent")
                    ),
                    ExpenseOverviewItem(
                        title = "Multiple Icons",
                        status = "success",
                        statusText = "Finalized",
                        datePaid = preconditionsAllStates.formattedToday10,
                        attributePreviewIcons = listOf("attachment", "multi-currency", "notes", "percent", "tax")
                    )
                ) { it.toExpenseOverviewItem() }
                
                // Expand and verify details for each expense
                staticItems[0].expandDetails()
                staticItems[0].detailsSection("Summary") {
                    shouldHaveAttribute("Status", "Finalized")
                    shouldHaveAttributeContaining("Category", "Delivery")
                    shouldHaveAttributeContaining("Date Paid", preconditionsAllStates.formattedToday1)
                    shouldHaveAttribute("Adjusted Amount for Tax Purposes", "USD 100.00")
                }
                staticItems[0].detailsSection("General Information") {
                    shouldHaveAttribute("Original Amount", "USD 100.00")
                }
                
                staticItems[1].expandDetails()
                staticItems[1].detailsSection("Summary") {
                    shouldHaveAttributeContaining("Status", "USD")
                    shouldHaveAttributeContaining("Category", "Delivery")
                    shouldHaveAttributeContaining("Date Paid", preconditionsAllStates.formattedToday2)
                    shouldHaveAttributeContaining("Adjusted Amount for Tax Purposes", "not provided")
                }
                staticItems[1].detailsSection("General Information") {
                    shouldHaveAttribute("Original Currency", "EUR")
                    shouldHaveAttribute("Original Amount", "EUR 50.00")
                }
                staticItems[1].detailsSection("Foreign Currency Conversion") {
                    shouldHaveAttributeContaining("Converted Amount", "not provided")
                    shouldHaveAttribute("Different Exchange Rate", "No")
                    shouldHaveAttributeContaining("Income Tax Amount", "not provided")
                }
                
                staticItems[3].expandDetails()
                staticItems[3].detailsSection("Notes") {
                    shouldHaveAttributeContaining("", "Important expense notes")
                }
                
                staticItems[4].expandDetails()
                staticItems[4].detailsSection("Summary") {
                    shouldHaveAttribute("General Tax", "VAT")
                    shouldHaveAttribute("General Tax Rate", "20.00%")
                    shouldHaveAttribute("General Tax Amount", "USD 20.00")
                }
                
                staticItems[5].expandDetails()
                staticItems[5].detailsSection("Attachments") {
                    shouldHaveAttributeContaining("", "Receipt 1")
                    shouldHaveAttributeContaining("", "Receipt 2")
                }
                
                staticItems[7].expandDetails()
                staticItems[7].detailsSection("Foreign Currency Conversion") {
                    shouldHaveAttribute("Converted Amount (USD)", "USD 9.00")
                    shouldHaveAttribute("Different Exchange Rate", "Yes")
                    shouldHaveAttribute("Income Tax Amount (USD)", "USD 8.50")
                }
                
                staticItems[8].expandDetails()
                staticItems[8].detailsSection("General Information") {
                    shouldHaveAttribute("Business Use", "70%")
                }
                
                staticItems[9].expandDetails()
                staticItems[9].detailsSection("Summary") {
                    shouldHaveAttribute("General Tax", "VAT")
                }
                staticItems[9].detailsSection("General Information") {
                    shouldHaveAttribute("Original Currency", "CHF")
                    shouldHaveAttribute("Business Use", "60%")
                }
                staticItems[9].detailsSection("Attachments") {
                    shouldHaveAttributeContaining("", "Receipt 1")
                }
                staticItems[9].detailsSection("Notes") {
                    shouldHaveAttributeContaining("", "Complex expense with all attributes")
                }
                
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
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
            
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
            val formattedToday1 = today.minusDays(1).format(formatter)
            val formattedToday2 = today.minusDays(2).format(formatter)
            val formattedToday3 = today.minusDays(3).format(formatter)
            val formattedToday4 = today.minusDays(4).format(formatter)
            val formattedToday5 = today.minusDays(5).format(formatter)
            val formattedToday6 = today.minusDays(6).format(formatter)
            val formattedToday7 = today.minusDays(7).format(formatter)
            val formattedToday8 = today.minusDays(8).format(formatter)
            val formattedToday9 = today.minusDays(9).format(formatter)
            val formattedToday10 = today.minusDays(10).format(formatter)

            init {
                // 1. Finalized expense in default currency (USD)
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Finalized USD",
                    datePaid = today.minusDays(1),
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
                    datePaid = today.minusDays(2),
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
                    datePaid = today.minusDays(3),
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
                    datePaid = today.minusDays(4),
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
                    datePaid = today.minusDays(5),
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
                    datePaid = today.minusDays(6),
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
                    datePaid = today.minusDays(7),
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
                    datePaid = today.minusDays(8),
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
                    datePaid = today.minusDays(9),
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
                    datePaid = today.minusDays(10),
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
                val today = LocalDate.now()
                (1..15).forEach { index ->
                    expense(
                        workspace = workspace,
                        category = category,
                        title = "Expense $index",
                        datePaid = today.minusDays(index.toLong()),
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
                val today = LocalDate.now()
                expense(
                    workspace = workspace,
                    category = officeCategory,
                    title = "Office",
                    datePaid = today.minusDays(1),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
                expense(
                    workspace = workspace,
                    category = travelCategory,
                    title = "Travel",
                    datePaid = today.minusDays(2),
                    originalAmount = 5000,
                    convertedAmounts = amountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(5000),
                    status = ExpenseStatus.FINALIZED
                )
                expense(
                    workspace = workspace,
                    category = mealsCategory,
                    title = "Meals",
                    datePaid = today.minusDays(3),
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
                        datePaid = today.minusDays(10L + index),
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
