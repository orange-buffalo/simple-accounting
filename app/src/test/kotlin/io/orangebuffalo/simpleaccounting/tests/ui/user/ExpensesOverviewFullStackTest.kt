package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.openExpensesOverviewPage
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExpensesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display expenses with all possible states and attributes`(page: Page) {
        page.authenticateViaCookie(preconditionsAllStates.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveExactItems(
                    "Finalized USD", "Pending Conversion EUR", "Pending Tax Conversion",
                    "With Notes", "With Tax", "With Attachments", "Foreign Currency Same Amounts",
                    "Foreign Currency Different Amounts", "Partial Business"
                ) { it.title!! }
                
                // Verify FINALIZED status
                staticItems[0].shouldHaveSuccessStatus("Finalized")
                staticItems[0].hasAttributePreviewIcons()
                
                // Verify PENDING_CONVERSION status
                staticItems[1].shouldHavePendingStatus("Pending")
                staticItems[1].hasAttributePreviewIcons("multi-currency")
                
                // Verify PENDING_CONVERSION_FOR_TAXATION_PURPOSES status
                staticItems[2].shouldHavePendingStatus("Pending")
                staticItems[2].hasAttributePreviewIcons("multi-currency")
                
                // Verify expense with notes icon
                staticItems[3].shouldHaveSuccessStatus("Finalized")
                staticItems[3].hasAttributePreviewIcons("notes")
                
                // Verify expense with general tax icon
                staticItems[4].shouldHaveSuccessStatus("Finalized")
                staticItems[4].hasAttributePreviewIcons("tax")
                
                // Verify expense with attachments icon
                staticItems[5].shouldHaveSuccessStatus("Finalized")
                staticItems[5].hasAttributePreviewIcons("attachment")
                
                // Verify foreign currency with same amounts
                staticItems[6].shouldHaveSuccessStatus("Finalized")
                staticItems[6].hasAttributePreviewIcons("multi-currency")
                
                // Verify foreign currency with different amounts for taxation
                staticItems[7].shouldHaveSuccessStatus("Finalized")
                staticItems[7].hasAttributePreviewIcons("multi-currency")
                
                // Verify partial business purpose icon
                staticItems[8].shouldHaveSuccessStatus("Finalized")
                staticItems[8].hasAttributePreviewIcons("percent")
                
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

            init {
                val today = LocalDate.now()
                
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
