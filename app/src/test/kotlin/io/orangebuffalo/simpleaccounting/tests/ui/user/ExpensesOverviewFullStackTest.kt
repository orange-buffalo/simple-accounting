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
    fun `should display single expense`(page: Page) {
        page.authenticateViaCookie(preconditionsSingleExpense.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveExactItems("Office Supplies") { it.title!! }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
        }
    }

    @Test
    fun `should display multiple expenses with different statuses`(page: Page) {
        page.authenticateViaCookie(preconditionsMultipleExpenses.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveExactItems(
                    "Finalized Expense", "Pending Conversion", "Pending Tax Conversion"
                ) { it.title!! }
                
                // Verify status labels
                staticItems[0].shouldHaveSuccessStatus("Finalized")
                staticItems[1].shouldHavePendingStatus("Pending")
                staticItems[2].shouldHavePendingStatus("Pending")
                
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

    private val preconditionsSingleExpense by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)

            init {
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Office Supplies",
                    datePaid = LocalDate.of(2020, 2, 10),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
            }
        }
    }

    private val preconditionsMultipleExpenses by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)

            init {
                val today = LocalDate.now()
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Finalized Expense",
                    datePaid = today.minusDays(3),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Pending Conversion",
                    datePaid = today.minusDays(2),
                    originalAmount = 5000,
                    convertedAmounts = emptyAmountsInDefaultCurrency(),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = ExpenseStatus.PENDING_CONVERSION
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Pending Tax Conversion",
                    datePaid = today.minusDays(1),
                    originalAmount = 3000,
                    convertedAmounts = amountsInDefaultCurrency(3000),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    useDifferentExchangeRateForIncomeTaxPurposes = true
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
