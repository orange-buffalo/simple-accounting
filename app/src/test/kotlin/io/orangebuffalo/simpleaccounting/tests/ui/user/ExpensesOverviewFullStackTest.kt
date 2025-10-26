package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpenseOverviewItem
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.openExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.toExpenseOverviewItem
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExpensesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display empty state when no expenses exist`(page: Page) {
        page.authenticateViaCookie(preconditionsEmpty.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveExactItems() { it.title!! }
            }
            reportRendering("expenses-overview.empty-state")
        }
    }

    @Test
    fun `should display single expense`(page: Page) {
        page.authenticateViaCookie(preconditionsSingleExpense.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveExactItems(
                    ExpenseOverviewItem(
                        title = "Office Supplies",
                        datePaid = preconditionsSingleExpense.calendarIconAttribute,
                        hasNotesIcon = false,
                        hasGeneralTaxIcon = false,
                        hasAttachmentsIcon = false,
                        hasForeignCurrencyIcon = false,
                        hasPartialBusinessPurposeIcon = false,
                    )
                ) { it.toExpenseOverviewItem() }
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
                    "Pending Conversion", "Pending Tax Conversion", "Finalized Expense"
                ) { it.title }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
            reportRendering("expenses-overview.multiple-statuses")
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
                shouldHaveExactItems(*firstPageExpenses) { it.title }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                }
                shouldHaveExactItems(
                    "Expense 5", "Expense 6", "Expense 7", "Expense 8", "Expense 9"
                ) { it.title }
                paginator {
                    previous()
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
                shouldHaveExactItems(*firstPageExpenses) { it.title }
            }
        }
    }

    @Test
    fun `should support filtering by free text search`(page: Page) {
        page.authenticateViaCookie(preconditionsFiltering.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldContainItems("Office", "Travel", "Meals") { it.title }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
            }
            filterInput { fill("office") }
            pageItems {
                shouldHaveExactItems("Office") { it.title }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
        }
    }

    @Test
    fun `should display all summary panel icons`(page: Page) {
        page.authenticateViaCookie(preconditionsSummaryIcons.fry)
        page.openExpensesOverviewPage {
            pageItems {
                val items = staticItems.map { it.toExpenseOverviewItem() }
                items.filter { it.hasNotesIcon }.map { it.title } shouldContainExactly listOf("With Notes")
                items.filter { it.hasGeneralTaxIcon }.map { it.title } shouldContainExactly listOf("With Tax")
                items.filter { it.hasAttachmentsIcon }.map { it.title } shouldContainExactly listOf("With Attachments")
                items.filter { it.hasForeignCurrencyIcon }.map { it.title } shouldContainExactly listOf("Foreign Currency")
                items.filter { it.hasPartialBusinessPurposeIcon }.map { it.title } shouldContainExactly listOf("Partial Business")
            }
            reportRendering("expenses-overview.summary-icons")
        }
    }

    @Test
    fun `should display expenses with attachments`(page: Page) {
        page.authenticateViaCookie(preconditionsAttachments.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveExactItems(
                    "Single Attachment", "Multiple Attachments", "No Attachments"
                ) { it.title }
            }
        }
    }

    @Test
    fun `should display expenses with foreign currency in different states`(page: Page) {
        page.authenticateViaCookie(preconditionsForeignCurrency.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveExactItems(
                    "Not Converted", "Same Amounts", "Different Amounts"
                ) { it.title }
            }
            reportRendering("expenses-overview.foreign-currency-states")
        }
    }

    @Test
    fun `should display expenses with and without general tax`(page: Page) {
        page.authenticateViaCookie(preconditionsGeneralTax.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveExactItems("With Tax", "Without Tax") { it.title }
            }
        }
    }

    @Test
    fun `should display expenses with partial business purpose`(page: Page) {
        page.authenticateViaCookie(preconditionsBusinessPurpose.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveExactItems(
                    "Fully Business", "Partial Business"
                ) { it.title }
            }
        }
    }

    @Test
    fun `should display expenses with and without notes`(page: Page) {
        page.authenticateViaCookie(preconditionsNotes.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveExactItems("With Notes", "Without Notes") { it.title }
            }
        }
    }

    @Test
    fun `should display loading state during API calls`(page: Page) {
        page.authenticateViaCookie(preconditionsEmpty.fry)
        page.withBlockedApiResponse(
            "workspaces/${preconditionsEmpty.workspace.id!!}/expenses*",
            initiator = {
                page.openExpensesOverviewPage()
            },
            blockedRequestSpec = {
                page.shouldBeExpensesOverviewPage {
                    reportRendering("expenses-overview.loading-state")
                }
            }
        )
    }

    @Test
    fun `should show create button only for editable workspace`(page: Page) {
        page.authenticateViaCookie(preconditionsEmpty.fry)
        page.openExpensesOverviewPage {
            createButton.shouldBeVisible()
        }
    }

    private val preconditionsEmpty by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }

    private val preconditionsSingleExpense by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)
            val calendarIconAttribute = io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.PrimaryAttribute(
                icon = "calendar",
                text = "Feb 10, 2020"
            )

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
                    datePaid = today.minusDays(1),
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
                    datePaid = today.minusDays(3),
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
                    status = ExpenseStatus.FINALIZED
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

    private val preconditionsSummaryIcons by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)
            val generalTax = generalTax(workspace = workspace)
            val document1 = document(workspace = workspace)

            init {
                val today = LocalDate.now()
                expense(
                    workspace = workspace,
                    category = category,
                    title = "With Notes",
                    datePaid = today.minusDays(1),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED,
                    notes = "Important notes"
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "With Tax",
                    datePaid = today.minusDays(2),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED,
                    generalTax = generalTax,
                    generalTaxRateInBps = 1000,
                    generalTaxAmount = 1000
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "With Attachments",
                    datePaid = today.minusDays(3),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf(document1)
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Foreign Currency",
                    datePaid = today.minusDays(4),
                    currency = "EUR",
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(12000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(12000),
                    status = ExpenseStatus.FINALIZED
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Partial Business",
                    datePaid = today.minusDays(5),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED,
                    percentOnBusiness = 50
                )
            }
        }
    }

    private val preconditionsAttachments by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)
            val document1 = document(workspace = workspace, name = "Receipt 1")
            val document2 = document(workspace = workspace, name = "Receipt 2")

            init {
                val today = LocalDate.now()
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Single Attachment",
                    datePaid = today.minusDays(1),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf(document1)
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Multiple Attachments",
                    datePaid = today.minusDays(2),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf(document1, document2)
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "No Attachments",
                    datePaid = today.minusDays(3),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
            }
        }
    }

    private val preconditionsForeignCurrency by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)

            init {
                val today = LocalDate.now()
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Not Converted",
                    datePaid = today.minusDays(1),
                    currency = "EUR",
                    originalAmount = 10000,
                    convertedAmounts = emptyAmountsInDefaultCurrency(),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = ExpenseStatus.PENDING_CONVERSION
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Same Amounts",
                    datePaid = today.minusDays(2),
                    currency = "EUR",
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(12000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(12000),
                    status = ExpenseStatus.FINALIZED
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Different Amounts",
                    datePaid = today.minusDays(3),
                    currency = "EUR",
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(12000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(11500),
                    status = ExpenseStatus.FINALIZED,
                    useDifferentExchangeRateForIncomeTaxPurposes = true
                )
            }
        }
    }

    private val preconditionsGeneralTax by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)
            val generalTax = generalTax(workspace = workspace)

            init {
                val today = LocalDate.now()
                expense(
                    workspace = workspace,
                    category = category,
                    title = "With Tax",
                    datePaid = today.minusDays(1),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED,
                    generalTax = generalTax,
                    generalTaxRateInBps = 1000,
                    generalTaxAmount = 1000
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Without Tax",
                    datePaid = today.minusDays(2),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
            }
        }
    }

    private val preconditionsBusinessPurpose by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)

            init {
                val today = LocalDate.now()
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Fully Business",
                    datePaid = today.minusDays(1),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED,
                    percentOnBusiness = 100
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Partial Business",
                    datePaid = today.minusDays(2),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED,
                    percentOnBusiness = 50
                )
            }
        }
    }

    private val preconditionsNotes by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)

            init {
                val today = LocalDate.now()
                expense(
                    workspace = workspace,
                    category = category,
                    title = "With Notes",
                    datePaid = today.minusDays(1),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED,
                    notes = "Important note here"
                )
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Without Notes",
                    datePaid = today.minusDays(2),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = ExpenseStatus.FINALIZED
                )
            }
        }
    }
}
