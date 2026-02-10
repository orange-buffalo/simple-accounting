package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainAll
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.previewIcons
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.primaryAttribute
import io.orangebuffalo.simpleaccounting.tests.infra.utils.dataValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.IncomesOverviewPage.Companion.openIncomesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.IncomesOverviewPage.Companion.shouldBeIncomesOverviewPage
import org.junit.jupiter.api.Test
import java.time.LocalDate

class IncomesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display incomes with all possible states and attributes`(page: Page) {
        page.authenticateViaCookie(preconditionsAllStates.fry)

        // Capture loading state by blocking API response
        page.withBlockedApiResponse(
            "**/incomes*",
            initiator = {
                page.openIncomesOverviewPage { }
            },
            blockedRequestSpec = {
                page.shouldBeIncomesOverviewPage {
                    pageItems.shouldHaveLoadingIndicatorVisible()
                    reportRendering("incomes-overview.loading")
                }
            }
        )

        page.shouldBeIncomesOverviewPage {
            pageItems {
                // Verify all incomes with their complete data
                shouldHaveExactData(
                    SaOverviewItemData(
                        title = "Finalized USD",
                        primaryAttributes = dateReceivedAsPrimary("15 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 100.00",
                    ),
                    SaOverviewItemData(
                        title = "Pending Conversion EUR",
                        primaryAttributes = dateReceivedAsPrimary("14 Jan 3025"),
                        middleColumnContent = pendingStatus(),
                        lastColumnContent = "EUR 50.00",
                        attributePreviewIcons = previewIcons(SaIconType.MULTI_CURRENCY),
                    ),
                    SaOverviewItemData(
                        title = "Pending Tax Conversion",
                        primaryAttributes = dateReceivedAsPrimary("13 Jan 3025"),
                        middleColumnContent = pendingStatus(),
                        lastColumnContent = "USD 40.00",
                        attributePreviewIcons = previewIcons(SaIconType.MULTI_CURRENCY),
                    ),
                    SaOverviewItemData(
                        title = "With Notes",
                        primaryAttributes = dateReceivedAsPrimary("12 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 20.00",
                        attributePreviewIcons = previewIcons(SaIconType.NOTES),
                    ),
                    SaOverviewItemData(
                        title = "With Tax",
                        primaryAttributes = dateReceivedAsPrimary("11 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 100.00",
                        attributePreviewIcons = previewIcons(SaIconType.TAX),
                    ),
                    SaOverviewItemData(
                        title = "With Attachments",
                        primaryAttributes = dateReceivedAsPrimary("10 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 50.00",
                        attributePreviewIcons = previewIcons(SaIconType.ATTACHMENT),
                    ),
                    SaOverviewItemData(
                        title = "Foreign Currency Same Amounts",
                        primaryAttributes = dateReceivedAsPrimary("9 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 60.00",
                        attributePreviewIcons = previewIcons(SaIconType.MULTI_CURRENCY),
                    ),
                    SaOverviewItemData(
                        title = "Foreign Currency Different Amounts",
                        primaryAttributes = dateReceivedAsPrimary("8 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 8.50",
                        attributePreviewIcons = previewIcons(SaIconType.MULTI_CURRENCY),
                    ),
                    SaOverviewItemData(
                        title = "Multiple Icons",
                        primaryAttributes = dateReceivedAsPrimary("7 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 160.00",
                        attributePreviewIcons = previewIcons(
                            SaIconType.NOTES,
                            SaIconType.TAX,
                            SaIconType.ATTACHMENT,
                            SaIconType.MULTI_CURRENCY,
                        ),
                    ),
                    SaOverviewItemData(
                        title = "With Invoice",
                        primaryAttributes = dateReceivedAsPrimary("6 Jan 3025"),
                        middleColumnContent = finalizedStatus(),
                        lastColumnContent = "USD 30.00",
                        attributePreviewIcons = previewIcons(SaIconType.INVOICE),
                    )
                )

                // Report rendering with all panels collapsed
                this@shouldBeIncomesOverviewPage.reportRendering("incomes-overview.loaded-collapsed")

                // Expand and verify details for each income
                staticItems[0].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to finalizedStatus(),
                        "Category" to "Delivery",
                        "Date Received" to "15 Jan 3025",
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
                        "Date Received" to "14 Jan 3025",
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
                        "Date Received" to "13 Jan 3025",
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
                        "Date Received" to "12 Jan 3025",
                        "Amount for Taxation Purposes" to "USD 20.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 20.00"
                    ),
                    DetailsSectionSpec(
                        title = "Additional Notes",
                        content = SaMarkdownOutput.markdownValue("Critical cargo payment notes")
                    )
                )

                staticItems[4].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to finalizedStatus(),
                        "Category" to "Delivery",
                        "Date Received" to "11 Jan 3025",
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
                        "Date Received" to "10 Jan 3025",
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
                        "Date Received" to "9 Jan 3025",
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
                        "Date Received" to "8 Jan 3025",
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
                        "Date Received" to "7 Jan 3025",
                        "Amount for Taxation Purposes" to "USD 160.00",
                        "Applicable General Tax" to "VAT",
                        "Applicable General Tax Rate" to "20%",
                        "Applicable General Tax Amount" to "USD 32.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Currency" to "CHF",
                        "Original Amount" to "CHF 150.00"
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
                        content = SaMarkdownOutput.markdownValue("Planet Express income with all attributes")
                    )
                )

                staticItems[9].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Status" to finalizedStatus(),
                        "Category" to "Delivery",
                        "Date Received" to "6 Jan 3025",
                        "Amount for Taxation Purposes" to "USD 30.00"
                    ),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Original Amount" to "USD 30.00",
                        "Associated Invoice" to "Invoice 1"
                    )
                )

                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }

            // Report rendering with all panels expanded
            reportRendering("incomes-overview.loaded-expanded")
        }
    }

    private fun actions() = listOf(SaActionLink.editActionLinkValue())

    private fun dateReceivedAsPrimary(dateReceived: String) =
        listOf(primaryAttribute(SaIconType.CALENDAR, text = dateReceived))

    private fun finalizedStatus() = dataValues(SaStatusLabel.successStatusValue(), "Finalized")

    private fun pendingStatus(label: String = "Pending") = dataValues(SaStatusLabel.pendingStatusValue(), label)

    @Test
    fun `should support pagination`(page: Page) {
        page.authenticateViaCookie(preconditionsPagination.fry)
        // Incomes are sorted by dateReceived descending (newest first)
        // Income 1 has date Jan 1 - 1 = Dec 31 (newest)
        // Income 2 has date Jan 1 - 2 = Dec 30
        // ...
        // Income 15 has date Jan 1 - 15 = Dec 17 (oldest)
        val firstPageIncomes = listOf(
            "Income 1", "Income 2", "Income 3", "Income 4", "Income 5",
            "Income 6", "Income 7", "Income 8", "Income 9", "Income 10"
        )
        val secondPageIncomes = listOf(
            "Income 11", "Income 12", "Income 13", "Income 14", "Income 15"
        )
        page.openIncomesOverviewPage {
            pageItems {
                shouldHaveTitles(firstPageIncomes)
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(secondPageIncomes)
                paginator {
                    previous()
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(firstPageIncomes)
            }
        }
    }

    @Test
    fun `should support filtering by free text search`(page: Page) {
        page.authenticateViaCookie(preconditionsFiltering.fry)
        page.openIncomesOverviewPage {
            pageItems {
                // ensure data loaded
                shouldHaveDataSatisfying { items ->
                    items.map { it.title }.shouldContainAll("Consulting", "Training", "Commission")
                }
                // ensure we update paginator as well
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
            }

            // Filter by title
            filterInput { fill("consulting") }
            pageItems {
                shouldHaveTitles("Consulting")
                // ensure paginator updated
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }

            // Filter by category name
            filterInput { fill("training") }
            pageItems {
                shouldHaveTitles("Training")
            }

            // Filter by notes
            filterInput { fill("Farnsworth") }
            pageItems {
                shouldHaveTitles("Commission")
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
            val customer = customer(workspace = workspace, name = "Customer 1")
            val invoice1 = invoice(customer = customer, title = "Invoice 1", currency = "USD")

            init {
                // 1. Finalized income in default currency (USD)
                income(
                    workspace = workspace,
                    category = category,
                    title = "Finalized USD",
                    dateReceived = LocalDate.of(3025, 1, 15),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = IncomeStatus.FINALIZED
                )

                // 2. Pending conversion - foreign currency not yet converted
                income(
                    workspace = workspace,
                    category = category,
                    title = "Pending Conversion EUR",
                    dateReceived = LocalDate.of(3025, 1, 14),
                    currency = "EUR",
                    originalAmount = 5000,
                    convertedAmounts = emptyAmountsInDefaultCurrency(),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = IncomeStatus.PENDING_CONVERSION
                )

                // 3. Pending conversion for taxation purposes
                income(
                    workspace = workspace,
                    category = category,
                    title = "Pending Tax Conversion",
                    dateReceived = LocalDate.of(3025, 1, 13),
                    currency = "GBP",
                    originalAmount = 3000,
                    convertedAmounts = amountsInDefaultCurrency(4000),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    useDifferentExchangeRateForIncomeTaxPurposes = true
                )

                // 4. With notes
                income(
                    workspace = workspace,
                    category = category,
                    title = "With Notes",
                    dateReceived = LocalDate.of(3025, 1, 12),
                    originalAmount = 2000,
                    convertedAmounts = amountsInDefaultCurrency(2000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(2000),
                    status = IncomeStatus.FINALIZED,
                    notes = "Critical cargo payment notes"
                )

                // 5. With general tax
                income(
                    workspace = workspace,
                    category = category,
                    title = "With Tax",
                    dateReceived = LocalDate.of(3025, 1, 11),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = IncomeStatus.FINALIZED,
                    generalTax = generalTax,
                    generalTaxRateInBps = 2000,
                    generalTaxAmount = 2000
                )

                // 6. With attachments (multiple)
                income(
                    workspace = workspace,
                    category = category,
                    title = "With Attachments",
                    dateReceived = LocalDate.of(3025, 1, 10),
                    originalAmount = 5000,
                    convertedAmounts = amountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(5000),
                    status = IncomeStatus.FINALIZED,
                    attachments = setOf(document1, document2)
                )

                // 7. Foreign currency with same amounts for reporting and taxation
                income(
                    workspace = workspace,
                    category = category,
                    title = "Foreign Currency Same Amounts",
                    dateReceived = LocalDate.of(3025, 1, 9),
                    currency = "CAD",
                    originalAmount = 8000,
                    convertedAmounts = amountsInDefaultCurrency(6000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(6000),
                    status = IncomeStatus.FINALIZED,
                    useDifferentExchangeRateForIncomeTaxPurposes = false
                )

                // 8. Foreign currency with different amounts for reporting and taxation
                income(
                    workspace = workspace,
                    category = category,
                    title = "Foreign Currency Different Amounts",
                    dateReceived = LocalDate.of(3025, 1, 8),
                    currency = "JPY",
                    originalAmount = 100000,
                    convertedAmounts = amountsInDefaultCurrency(900),
                    incomeTaxableAmounts = amountsInDefaultCurrency(850),
                    status = IncomeStatus.FINALIZED,
                    useDifferentExchangeRateForIncomeTaxPurposes = true
                )

                // 9. Multiple icons - income with all attributes (no invoice, as invoice is separate)
                income(
                    workspace = workspace,
                    category = category,
                    title = "Multiple Icons",
                    dateReceived = LocalDate.of(3025, 1, 7),
                    currency = "CHF",
                    originalAmount = 15000,
                    convertedAmounts = amountsInDefaultCurrency(16000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(16000),
                    status = IncomeStatus.FINALIZED,
                    generalTax = generalTax,
                    generalTaxRateInBps = 2000,
                    generalTaxAmount = 3200,
                    attachments = setOf(document1),
                    notes = "Planet Express income with all attributes",
                    useDifferentExchangeRateForIncomeTaxPurposes = false
                )

                // 10. With linked invoice
                income(
                    workspace = workspace,
                    category = category,
                    title = "With Invoice",
                    dateReceived = LocalDate.of(3025, 1, 6),
                    originalAmount = 3000,
                    convertedAmounts = amountsInDefaultCurrency(3000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(3000),
                    status = IncomeStatus.FINALIZED,
                    linkedInvoice = invoice1
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
                    income(
                        workspace = workspace,
                        category = category,
                        title = "Income $index",
                        dateReceived = baseDate.minusDays(index.toLong()),
                        originalAmount = 1000 * index.toLong(),
                        convertedAmounts = amountsInDefaultCurrency(1000 * index.toLong()),
                        incomeTaxableAmounts = amountsInDefaultCurrency(1000 * index.toLong()),
                        status = IncomeStatus.FINALIZED
                    )
                }
            }
        }
    }

    private val preconditionsFiltering by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val consultingCategory = category(workspace = workspace, name = "Consulting")
            val trainingCategory = category(workspace = workspace, name = "Training")
            val commissionCategory = category(workspace = workspace, name = "Commission")
            val otherCategory = category(workspace = workspace, name = "Other")

            init {
                val baseDate = LocalDate.of(3025, 1, 1)
                income(
                    workspace = workspace,
                    category = consultingCategory,
                    title = "Consulting",
                    dateReceived = baseDate.minusDays(1),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    status = IncomeStatus.FINALIZED
                )
                income(
                    workspace = workspace,
                    category = trainingCategory,
                    title = "Training",
                    dateReceived = baseDate.minusDays(2),
                    originalAmount = 5000,
                    convertedAmounts = amountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(5000),
                    status = IncomeStatus.FINALIZED
                )
                income(
                    workspace = workspace,
                    category = commissionCategory,
                    title = "Commission",
                    dateReceived = baseDate.minusDays(3),
                    originalAmount = 3000,
                    convertedAmounts = amountsInDefaultCurrency(3000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(3000),
                    status = IncomeStatus.FINALIZED,
                    notes = "This is a Farnsworth priority"
                )
                (1..10).forEach { index ->
                    income(
                        workspace = workspace,
                        category = otherCategory,
                        title = "Other $index",
                        dateReceived = baseDate.minusDays(10L + index),
                        originalAmount = 1000,
                        convertedAmounts = amountsInDefaultCurrency(1000),
                        incomeTaxableAmounts = amountsInDefaultCurrency(1000),
                        status = IncomeStatus.FINALIZED
                    )
                }
            }
        }
    }
}
