package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.previewIcons
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.primaryAttribute
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateExpensePage.Companion.shouldBeCreateExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.openExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExpensesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display expenses with all possible states and attributes`(page: Page) {
        page.authenticateViaCookie(preconditionsAllStates.fry)

        // Capture loading state by blocking API response
        page.withBlockedApiResponse(
            "**/expenses*",
            initiator = {
                page.openExpensesOverviewPage { }
            },
            blockedRequestSpec = {
                page.shouldBeExpensesOverviewPage {
                    pageItems.shouldHaveLoadingIndicatorVisible()
                    reportRendering("expenses-overview.loading")
                }
            }
        )

        page.shouldBeExpensesOverviewPage {
            pageItems {
                // Verify all expenses with their complete data
                shouldHaveExactData(
                    SaOverviewItemData(
                        title = "Finalized USD",
                        primaryAttributes = datePaidAsPrimary("15 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 100.00",
                    ),
                    SaOverviewItemData(
                        title = "Pending Conversion EUR",
                        primaryAttributes = datePaidAsPrimary("14 Jan 3025"),
                        middleColumnContent = pendingStatus(),
                        lastColumnContent = "EUR 50.00",
                        attributePreviewIcons = previewIcons(SaIconType.MULTI_CURRENCY),
                    ),
                    SaOverviewItemData(
                        title = "Pending Tax Conversion",
                        primaryAttributes = datePaidAsPrimary("13 Jan 3025"),
                        middleColumnContent = pendingStatus(),
                        lastColumnContent = "USD 40.00",
                        attributePreviewIcons = previewIcons(SaIconType.MULTI_CURRENCY),
                    ),
                    SaOverviewItemData(
                        title = "With Notes",
                        primaryAttributes = datePaidAsPrimary("12 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 20.00",
                        attributePreviewIcons = previewIcons(SaIconType.NOTES),
                    ),
                    SaOverviewItemData(
                        title = "With Tax",
                        primaryAttributes = datePaidAsPrimary("11 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 100.00",
                        attributePreviewIcons = previewIcons(SaIconType.TAX),
                    ),
                    SaOverviewItemData(
                        title = "With Attachments",
                        primaryAttributes = datePaidAsPrimary("10 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 50.00",
                        attributePreviewIcons = previewIcons(SaIconType.ATTACHMENT),
                    ),
                    SaOverviewItemData(
                        title = "Foreign Currency Same Amounts",
                        primaryAttributes = datePaidAsPrimary("9 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 60.00",
                        attributePreviewIcons = previewIcons(SaIconType.MULTI_CURRENCY),
                    ),
                    SaOverviewItemData(
                        title = "Foreign Currency Different Amounts",
                        primaryAttributes = datePaidAsPrimary("8 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 8.50",
                        attributePreviewIcons = previewIcons(SaIconType.MULTI_CURRENCY),
                    ),
                    SaOverviewItemData(
                        title = "Partial Business",
                        primaryAttributes = datePaidAsPrimary("7 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 40.00",
                        attributePreviewIcons = previewIcons(SaIconType.PERCENT),
                    ),
                    SaOverviewItemData(
                        title = "Multiple Icons",
                        primaryAttributes = datePaidAsPrimary("6 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 160.00",
                        attributePreviewIcons = previewIcons(
                            SaIconType.NOTES,
                            SaIconType.TAX,
                            SaIconType.ATTACHMENT,
                            SaIconType.MULTI_CURRENCY,
                            SaIconType.PERCENT,
                        ),
                    )
                )

                // Report rendering with all panels collapsed
                this@shouldBeExpensesOverviewPage.reportRendering("expenses-overview.loaded-collapsed")

                // Expand and verify details for each expense
                staticItems[0].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to finalizedStatus(),
                        "Category" to "Delivery",
                        "Date Paid" to "15 Jan 3025",
                        "Amount for Taxation Purposes" to "USD 100.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 100.00"
                    )
                )

                staticItems[1].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to pendingStatus("Conversion to USD pending"),
                        "Category" to "Delivery",
                        "Date Paid" to "14 Jan 3025",
                        "Amount for Taxation Purposes" to "Not yet provided"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Currency" to "EUR",
                        "Original Amount" to "EUR 50.00"
                    ),
                    DetailsSectionSpec(
                        title = "Currency Conversion",
                        "Amount in USD" to "Not yet available",
                        "Using different exchange rate for taxation purposes?" to "No",
                        "Amount in USD for taxation purposes" to "Not yet available"
                    )
                )

                staticItems[2].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to pendingStatus("Waiting for exchange rate"),
                        "Category" to "Delivery",
                        "Date Paid" to "13 Jan 3025",
                        "Amount for Taxation Purposes" to "Not yet provided"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Currency" to "GBP",
                        "Original Amount" to "GBP 30.00"
                    ),
                    DetailsSectionSpec(
                        title = "Currency Conversion",
                        "Amount in USD" to "USD 40.00",
                        "Using different exchange rate for taxation purposes?" to "Yes",
                        "Amount in USD for taxation purposes" to "Not yet available"
                    )
                )

                staticItems[3].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to finalizedStatus(),
                        "Category" to "Delivery",
                        "Date Paid" to "12 Jan 3025",
                        "Amount for Taxation Purposes" to "USD 20.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 20.00"
                    ),
                    DetailsSectionSpec(
                        title = "Additional Notes",
                        content = SaMarkdownOutput.markdownValue("Critical Slurm delivery notes")
                    )
                )

                staticItems[4].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to finalizedStatus(),
                        "Category" to "Delivery",
                        "Date Paid" to "11 Jan 3025",
                        "Amount for Taxation Purposes" to "USD 100.00",
                        "Applicable General Tax" to "VAT",
                        "Applicable General Tax Rate" to "20%",
                        "Applicable General Tax Amount" to "USD 20.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 100.00"
                    )
                )

                staticItems[5].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to finalizedStatus(),
                        "Category" to "Delivery",
                        "Date Paid" to "10 Jan 3025",
                        "Amount for Taxation Purposes" to "USD 50.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 50.00"
                    ),
                    DetailsSectionSpec(
                        title = "Attachments",
                        content = SaDocumentsList.documentsValue("Receipt 1", "Receipt 2")
                    )
                )

                staticItems[6].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to finalizedStatus(),
                        "Category" to "Delivery",
                        "Date Paid" to "9 Jan 3025",
                        "Amount for Taxation Purposes" to "USD 60.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Currency" to "CAD",
                        "Original Amount" to "CAD 80.00"
                    ),
                    DetailsSectionSpec(
                        title = "Currency Conversion",
                        "Amount in USD" to "USD 60.00",
                        "Using different exchange rate for taxation purposes?" to "No",
                        "Amount in USD for taxation purposes" to "USD 60.00"
                    )
                )

                staticItems[7].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to finalizedStatus(),
                        "Category" to "Delivery",
                        "Date Paid" to "8 Jan 3025",
                        "Amount for Taxation Purposes" to "USD 8.50"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Currency" to "JPY",
                        "Original Amount" to "JPY 100,000"
                    ),
                    DetailsSectionSpec(
                        title = "Currency Conversion",
                        "Amount in USD" to "USD 9.00",
                        "Using different exchange rate for taxation purposes?" to "Yes",
                        "Amount in USD for taxation purposes" to "USD 8.50"
                    )
                )

                staticItems[8].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to finalizedStatus(),
                        "Category" to "Delivery",
                        "Date Paid" to "7 Jan 3025",
                        "Amount for Taxation Purposes" to "USD 40.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 40.00",
                        "Partial Business Purpose" to "70% related to business activities"
                    )
                )

                staticItems[9].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to finalizedStatus(),
                        "Category" to "Delivery",
                        "Date Paid" to "6 Jan 3025",
                        "Amount for Taxation Purposes" to "USD 160.00",
                        "Applicable General Tax" to "VAT",
                        "Applicable General Tax Rate" to "20%",
                        "Applicable General Tax Amount" to "USD 32.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Currency" to "CHF",
                        "Original Amount" to "CHF 150.00",
                        "Partial Business Purpose" to "60% related to business activities"
                    ),
                    DetailsSectionSpec(
                        title = "Currency Conversion",
                        "Amount in USD" to "USD 160.00",
                        "Using different exchange rate for taxation purposes?" to "No",
                        "Amount in USD for taxation purposes" to "USD 160.00"
                    ),
                    DetailsSectionSpec(
                        title = "Attachments",
                        content = SaDocumentsList.documentsValue("Receipt 1")
                    ),
                    DetailsSectionSpec(
                        title = "Additional Notes",
                        content = SaMarkdownOutput.markdownValue("Planet Express delivery with all attributes")
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

    private fun actions() = listOf(SaActionLink.copyActionLinkValue(), SaActionLink.editActionLinkValue())

    private fun datePaidAsPrimary(datePaid: String) =
        listOf(primaryAttribute(SaIconType.CALENDAR, text = datePaid))

    private fun finalizedStatus() = dataValues(SaStatusLabel.successStatusValue(), "Finalized")

    private fun pendingStatus(label: String = "Pending") = dataValues(SaStatusLabel.pendingStatusValue(), label)

    @Test
    fun `should support pagination`(page: Page) {
        page.authenticateViaCookie(preconditionsPagination.fry)
        // Expenses are sorted by datePaid descending (newest first)
        // Expense 1 has date Jan 1 - 1 = Dec 31 (newest)
        // Expense 2 has date Jan 1 - 2 = Dec 30
        // ...
        // Expense 15 has date Jan 1 - 15 = Dec 17 (oldest)
        val firstPageExpenses = listOf(
            "Expense 1", "Expense 2", "Expense 3", "Expense 4", "Expense 5",
            "Expense 6", "Expense 7", "Expense 8", "Expense 9", "Expense 10"
        )
        val secondPageExpenses = listOf(
            "Expense 11", "Expense 12", "Expense 13", "Expense 14", "Expense 15"
        )
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveTitles(firstPageExpenses)
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(secondPageExpenses)
                paginator {
                    previous()
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(firstPageExpenses)
            }
        }
    }

    @Test
    fun `should support filtering by free text search`(page: Page) {
        page.authenticateViaCookie(preconditionsFiltering.fry)
        page.openExpensesOverviewPage {
            pageItems {
                // ensure data loaded
                shouldHaveDataSatisfying { items ->
                    items.map { it.title }.shouldContainAll("Office", "Travel", "Meals")
                }
                // ensure we update paginator as well
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
            }

            // Filter by title
            filterInput { fill("office") }
            pageItems {
                shouldHaveTitles("Office")
                // ensure paginator updated
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }

            // Filter by category name
            filterInput { fill("travel") }
            pageItems {
                shouldHaveTitles("Travel")
            }

            // Filter by notes
            filterInput { fill("Zoidberg") }
            pageItems {
                shouldHaveTitles("Meals")
            }
        }
    }

    private val preconditionsAllStates by lazyPreconditions {
        object {
            val fry = platformUser(
                userName = "Fry",
                isAdmin = false,
                activated = true,
                documentsStorage = "noop"
            )
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
                    datePaid = LocalDate.of(3025, 1, 15),
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
                    datePaid = LocalDate.of(3025, 1, 14),
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
                    datePaid = LocalDate.of(3025, 1, 13),
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
                    datePaid = LocalDate.of(3025, 1, 12),
                    originalAmount = 2000,
                    convertedAmounts = amountsInDefaultCurrency(2000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(2000),
                    status = ExpenseStatus.FINALIZED,
                    notes = "Critical Slurm delivery notes"
                )

                // 5. With general tax
                expense(
                    workspace = workspace,
                    category = category,
                    title = "With Tax",
                    datePaid = LocalDate.of(3025, 1, 11),
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
                    datePaid = LocalDate.of(3025, 1, 10),
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
                    datePaid = LocalDate.of(3025, 1, 9),
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
                    datePaid = LocalDate.of(3025, 1, 8),
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
                    datePaid = LocalDate.of(3025, 1, 7),
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
                    datePaid = LocalDate.of(3025, 1, 6),
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
                    notes = "Planet Express delivery with all attributes",
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
                val baseDate = LocalDate.of(3025, 1, 1)
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
            val otherCategory = category(workspace = workspace, name = "Other")

            init {
                val baseDate = LocalDate.of(3025, 1, 1)
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
                    notes = "This is a Zoidberg emergency"
                )
                (1..10).forEach { index ->
                    expense(
                        workspace = workspace,
                        category = otherCategory,
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

    @Test
    fun `should open create expense page with default values from create button`(page: Page) {
        page.authenticateViaCookie(preconditionsActions.fry)
        page.openExpensesOverviewPage {
            createButton.click()
        }

        page.shouldBeCreateExpensePage {
            category {
                input.shouldHaveSelectedValue("Select a category")
            }
            title {
                input.shouldHaveValue("")
            }
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
            }
            originalAmount {
                input.shouldHaveValue("")
            }
            datePaid {
                input.shouldHaveValue("1999-03-28")
            }
            generalTax {
                input.shouldHaveSelectedValue("Select a tax")
            }
            notes {
                input.shouldHaveValue("")
            }

            partialForBusiness().shouldNotBeChecked()
            percentOnBusiness().shouldBeHidden()

            convertedAmountInDefaultCurrency("USD").shouldBeHidden()
            useDifferentExchangeRateForIncomeTaxPurposes().shouldBeHidden()
            incomeTaxableAmountInDefaultCurrency("USD").shouldBeHidden()
        }
    }

    @Test
    fun `should open edit expense page with loaded values from edit action`(page: Page) {
        page.authenticateViaCookie(preconditionsActions.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveTitles("Spaceship fuel refill")
                staticItems[0].executeEditAction()
            }
        }

        page.shouldBeEditExpensePage {
            category {
                input.shouldHaveSelectedValue("Travel")
            }
            title {
                input.shouldHaveValue("Spaceship fuel refill")
            }
            currency {
                input.shouldHaveSelectedValue("GBP - British Pound")
            }
            originalAmount {
                input.shouldHaveValue("500.00")
            }
            datePaid {
                input.shouldHaveValue("3025-01-15")
            }
            generalTax {
                input.shouldHaveSelectedValue("VAT")
            }
            notes {
                input.shouldHaveValue("Intergalactic delivery mission")
            }

            convertedAmountInDefaultCurrency("USD").shouldBeVisible()
            convertedAmountInDefaultCurrency("USD").input.shouldHaveValue("625.00")

            useDifferentExchangeRateForIncomeTaxPurposes().shouldNotBeChecked()
            incomeTaxableAmountInDefaultCurrency("USD").shouldBeHidden()

            partialForBusiness().shouldBeChecked()
            percentOnBusiness().shouldBeVisible()
            percentOnBusiness().input.shouldHaveValue("80")
        }
    }

    @Test
    fun `should open create expense page with pre-populated values from copy action and create new expense`(page: Page) {
        page.authenticateViaCookie(preconditionsActions.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveTitles("Spaceship fuel refill")
                staticItems[0].executeCopyAction()
            }
        }

        page.shouldBeCreateExpensePage {
            category {
                input.shouldHaveSelectedValue("Travel")
            }
            title {
                input.shouldHaveValue("Spaceship fuel refill")
            }
            currency {
                input.shouldHaveSelectedValue("GBP - British Pound")
            }
            originalAmount {
                input.shouldHaveValue("500.00")
            }
            datePaid {
                input.shouldHaveValue("")
            }
            generalTax {
                input.shouldHaveSelectedValue("VAT")
            }
            notes {
                input.shouldHaveValue("Intergalactic delivery mission")
            }

            convertedAmountInDefaultCurrency("USD").shouldBeVisible()
            convertedAmountInDefaultCurrency("USD").input.shouldHaveValue("")

            useDifferentExchangeRateForIncomeTaxPurposes().shouldNotBeChecked()
            incomeTaxableAmountInDefaultCurrency("USD").shouldBeHidden()

            partialForBusiness().shouldBeChecked()
            percentOnBusiness().shouldBeVisible()
            percentOnBusiness().input.shouldHaveValue("80")

            // Fill in the missing fields
            datePaid { input.fill("3025-02-01") }
            convertedAmountInDefaultCurrency("USD").input.fill("625.00")
            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        val expenses = aggregateTemplate.findAll<Expense>()
        expenses.shouldHaveSize(2)

        val copiedExpense = expenses.first { it.title == "Spaceship fuel refill" && it.datePaid == LocalDate.of(3025, 2, 1) }
        copiedExpense.shouldBeEntityWithFields(
            Expense(
                title = "Spaceship fuel refill",
                categoryId = preconditionsActions.category.id!!,
                datePaid = LocalDate.of(3025, 2, 1),
                currency = "GBP",
                originalAmount = 50000,
                convertedAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = 62500,
                    adjustedAmountInDefaultCurrency = 41667
                ),
                incomeTaxableAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = 62500,
                    adjustedAmountInDefaultCurrency = 41667
                ),
                status = ExpenseStatus.FINALIZED,
                percentOnBusiness = 80,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                timeRecorded = MOCK_TIME,
                workspaceId = preconditionsActions.workspace.id!!,
                generalTaxId = preconditionsActions.generalTax.id,
                generalTaxAmount = 8333,
                generalTaxRateInBps = 2000,
                notes = "Intergalactic delivery mission",
            ),
            ignoredProperties = arrayOf(
                Expense::id,
                Expense::version,
            )
        )
    }

    private val preconditionsActions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace, name = "Travel")
            val generalTax = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
            val expense = expense(
                workspace = workspace,
                category = category,
                title = "Spaceship fuel refill",
                datePaid = LocalDate.of(3025, 1, 15),
                currency = "GBP",
                originalAmount = 50000,
                convertedAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = 62500,
                    adjustedAmountInDefaultCurrency = 50000
                ),
                incomeTaxableAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = 62500,
                    adjustedAmountInDefaultCurrency = 50000
                ),
                status = ExpenseStatus.FINALIZED,
                percentOnBusiness = 80,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                generalTax = generalTax,
                generalTaxRateInBps = 2000,
                generalTaxAmount = 12500,
                notes = "Intergalactic delivery mission"
            )
        }
    }
}
