package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainAll
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.previewIcons
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.primaryAttribute
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.IncomeTaxPaymentsOverviewPage.Companion.openIncomeTaxPaymentsOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.IncomeTaxPaymentsOverviewPage.Companion.shouldBeIncomeTaxPaymentsOverviewPage
import org.junit.jupiter.api.Test
import java.time.LocalDate

class IncomeTaxPaymentsOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display income tax payments with all possible attributes`(page: Page) {
        page.authenticateViaCookie(preconditionsAllStates.fry)

        // Capture loading state by blocking API response
        page.withBlockedApiResponse(
            "**/income-tax-payments*",
            initiator = {
                page.openIncomeTaxPaymentsOverviewPage { }
            },
            blockedRequestSpec = {
                page.shouldBeIncomeTaxPaymentsOverviewPage {
                    pageItems.shouldHaveLoadingIndicatorVisible()
                    reportRendering("income-tax-payments-overview.loading")
                }
            }
        )

        page.shouldBeIncomeTaxPaymentsOverviewPage {
            pageItems {
                // Verify all income tax payments with their complete data
                shouldHaveExactData(
                    SaOverviewItemData(
                        title = "Basic Payment",
                        primaryAttributes = datePaidAsPrimary("15 Jan 3025"),
                        lastColumnContent = "USD 100.00",
                    ),
                    SaOverviewItemData(
                        title = "With Notes",
                        primaryAttributes = datePaidAsPrimary("14 Jan 3025"),
                        lastColumnContent = "USD 50.00",
                        attributePreviewIcons = previewIcons(SaIconType.NOTES),
                    ),
                    SaOverviewItemData(
                        title = "With Attachments",
                        primaryAttributes = datePaidAsPrimary("13 Jan 3025"),
                        lastColumnContent = "USD 75.00",
                        attributePreviewIcons = previewIcons(SaIconType.ATTACHMENT),
                    ),
                    SaOverviewItemData(
                        title = "With All Attributes",
                        primaryAttributes = datePaidAsPrimary("12 Jan 3025"),
                        lastColumnContent = "USD 200.00",
                        attributePreviewIcons = previewIcons(SaIconType.NOTES, SaIconType.ATTACHMENT),
                    )
                )

                // Report rendering with all panels collapsed
                this@shouldBeIncomeTaxPaymentsOverviewPage.reportRendering("income-tax-payments-overview.loaded-collapsed")

                // Expand and verify details for each payment
                staticItems[0].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Date paid" to "15 Jan 3025",
                        "Reporting Date" to "15 Jan 3025"
                    )
                )

                staticItems[1].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Date paid" to "14 Jan 3025",
                        "Reporting Date" to "14 Jan 3025"
                    ),
                    DetailsSectionSpec(
                        title = "Additional Notes",
                        content = SaMarkdownOutput.markdownValue("Important payment notes")
                    )
                )

                staticItems[2].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Date paid" to "13 Jan 3025",
                        "Reporting Date" to "13 Jan 3025"
                    ),
                    DetailsSectionSpec(
                        title = "Attachments",
                        content = SaDocumentsList.documentsValue("Receipt 1", "Receipt 2")
                    )
                )

                staticItems[3].shouldHaveDetails(
                    actions = actions(),
                    DetailsSectionSpec(
                        title = "Summary",
                        "Date paid" to "12 Jan 3025",
                        "Reporting Date" to "12 Jan 3025"
                    ),
                    DetailsSectionSpec(
                        title = "Attachments",
                        content = SaDocumentsList.documentsValue("Receipt 1")
                    ),
                    DetailsSectionSpec(
                        title = "Additional Notes",
                        content = SaMarkdownOutput.markdownValue("Planet Express tax payment with all attributes")
                    )
                )

                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }

            // Report rendering with all panels expanded
            reportRendering("income-tax-payments-overview.loaded-expanded")
        }
    }

    private fun actions() = listOf(SaActionLink.editActionLinkValue())

    private fun datePaidAsPrimary(datePaid: String) =
        listOf(primaryAttribute(SaIconType.CALENDAR, text = datePaid))

    @Test
    fun `should support pagination`(page: Page) {
        page.authenticateViaCookie(preconditionsPagination.fry)
        // Income tax payments are sorted by datePaid descending (newest first), then by timeRecorded descending
        // Payment 1 has date Jan 1 - 1 = Dec 31 (newest)
        // Payment 2 has date Jan 1 - 2 = Dec 30
        // ...
        // Payment 15 has date Jan 1 - 15 = Dec 17 (oldest)
        val firstPagePayments = listOf(
            "Payment 1", "Payment 2", "Payment 3", "Payment 4", "Payment 5",
            "Payment 6", "Payment 7", "Payment 8", "Payment 9", "Payment 10"
        )
        val secondPagePayments = listOf(
            "Payment 11", "Payment 12", "Payment 13", "Payment 14", "Payment 15"
        )
        page.openIncomeTaxPaymentsOverviewPage {
            pageItems {
                shouldHaveTitles(firstPagePayments)
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(secondPagePayments)
                paginator {
                    previous()
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(firstPagePayments)
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
            val document1 = document(workspace = workspace, name = "Receipt 1")
            val document2 = document(workspace = workspace, name = "Receipt 2")

            init {
                // 1. Basic payment with minimum data
                incomeTaxPayment(
                    workspace = workspace,
                    title = "Basic Payment",
                    datePaid = LocalDate.of(3025, 1, 15),
                    reportingDate = LocalDate.of(3025, 1, 15),
                    amount = 10000
                )

                // 2. Payment with notes
                incomeTaxPayment(
                    workspace = workspace,
                    title = "With Notes",
                    datePaid = LocalDate.of(3025, 1, 14),
                    reportingDate = LocalDate.of(3025, 1, 14),
                    amount = 5000,
                    notes = "Important payment notes"
                )

                // 3. Payment with attachments (multiple)
                incomeTaxPayment(
                    workspace = workspace,
                    title = "With Attachments",
                    datePaid = LocalDate.of(3025, 1, 13),
                    reportingDate = LocalDate.of(3025, 1, 13),
                    amount = 7500,
                    attachments = setOf(document1, document2)
                )

                // 4. Payment with all attributes
                incomeTaxPayment(
                    workspace = workspace,
                    title = "With All Attributes",
                    datePaid = LocalDate.of(3025, 1, 12),
                    reportingDate = LocalDate.of(3025, 1, 12),
                    amount = 20000,
                    attachments = setOf(document1),
                    notes = "Planet Express tax payment with all attributes"
                )
            }
        }
    }

    private val preconditionsPagination by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)

            init {
                val baseDate = LocalDate.of(3025, 1, 1)
                (1..15).forEach { index ->
                    incomeTaxPayment(
                        workspace = workspace,
                        title = "Payment $index",
                        datePaid = baseDate.minusDays(index.toLong()),
                        reportingDate = baseDate.minusDays(index.toLong()),
                        amount = 1000 * index.toLong()
                    )
                }
            }
        }
    }
}
