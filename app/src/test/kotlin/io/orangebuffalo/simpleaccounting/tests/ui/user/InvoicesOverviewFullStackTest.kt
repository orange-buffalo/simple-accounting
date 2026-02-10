package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainAll
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.previewIcons
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.primaryAttribute
import io.orangebuffalo.simpleaccounting.tests.infra.utils.dataValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.InvoicesOverviewPage.Companion.openInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.InvoicesOverviewPage.Companion.shouldBeInvoicesOverviewPage
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InvoicesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display invoices with all possible states and attributes`(page: Page) {
        page.authenticateViaCookie(preconditionsAllStates.fry)

        // Capture loading state by blocking API response
        page.withBlockedApiResponse(
            "**/invoices*",
            initiator = {
                page.openInvoicesOverviewPage { }
            },
            blockedRequestSpec = {
                page.shouldBeInvoicesOverviewPage {
                    pageItems.shouldHaveLoadingIndicatorVisible()
                    reportRendering("invoices-overview.loading")
                }
            }
        )

        page.shouldBeInvoicesOverviewPage {
            pageItems {
                // Verify all invoices with their complete data
                shouldHaveExactData(
                    SaOverviewItemData(
                        title = "Invoice With All Attributes",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.CUSTOMER, text = "Space Customer"),
                            primaryAttribute(SaIconType.CALENDAR, text = "6 Jan 3025")
                        ),
                        middleColumnContent = paidStatus(),
                        lastColumnContent = "EUR 140.00",
                        attributePreviewIcons = previewIcons(
                            SaIconType.NOTES,
                            SaIconType.ATTACHMENT,
                            SaIconType.TAX,
                            SaIconType.MULTI_CURRENCY,
                        ),
                    ),
                    SaOverviewItemData(
                        title = "Invoice With Notes",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.CUSTOMER, text = "Space Customer"),
                            primaryAttribute(SaIconType.CALENDAR, text = "7 Jan 3025")
                        ),
                        middleColumnContent = paidStatus(),
                        lastColumnContent = "USD 60.00",
                        attributePreviewIcons = previewIcons(SaIconType.NOTES),
                    ),
                    SaOverviewItemData(
                        title = "Invoice With Attachments",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.CUSTOMER, text = "Space Customer"),
                            primaryAttribute(SaIconType.CALENDAR, text = "8 Jan 3025")
                        ),
                        middleColumnContent = paidStatus(),
                        lastColumnContent = "USD 90.00",
                        attributePreviewIcons = previewIcons(SaIconType.ATTACHMENT),
                    ),
                    SaOverviewItemData(
                        title = "Invoice With Tax",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.CUSTOMER, text = "Space Customer"),
                            primaryAttribute(SaIconType.CALENDAR, text = "9 Jan 3025")
                        ),
                        middleColumnContent = paidStatus(),
                        lastColumnContent = "USD 120.00",
                        attributePreviewIcons = previewIcons(SaIconType.TAX),
                    ),
                    SaOverviewItemData(
                        title = "Foreign Currency Invoice",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.CUSTOMER, text = "Space Customer"),
                            primaryAttribute(SaIconType.CALENDAR, text = "10 Jan 3025")
                        ),
                        middleColumnContent = paidStatus(),
                        lastColumnContent = "EUR 80.00",
                        attributePreviewIcons = previewIcons(SaIconType.MULTI_CURRENCY),
                    ),
                    SaOverviewItemData(
                        title = "Cancelled Invoice",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.CUSTOMER, text = "Space Customer")
                        ),
                        middleColumnContent = cancelledStatus(),
                        lastColumnContent = "USD 50.00",
                    ),
                    SaOverviewItemData(
                        title = "Draft Invoice",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.CUSTOMER, text = "Space Customer")
                        ),
                        middleColumnContent = draftStatus(),
                        lastColumnContent = "USD 75.00",
                    ),
                    SaOverviewItemData(
                        title = "Overdue Invoice",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.CUSTOMER, text = "Space Customer")
                        ),
                        middleColumnContent = overdueStatus(),
                        lastColumnContent = "USD 150.00",
                    ),
                    SaOverviewItemData(
                        title = "Sent Invoice",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.CUSTOMER, text = "Space Customer")
                        ),
                        middleColumnContent = sentStatus(),
                        lastColumnContent = "USD 200.00",
                    ),
                    SaOverviewItemData(
                        title = "Paid Invoice",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.CUSTOMER, text = "Space Customer"),
                            primaryAttribute(SaIconType.CALENDAR, text = "15 Jan 3025")
                        ),
                        middleColumnContent = paidStatus(),
                        lastColumnContent = "USD 100.00",
                    )
                )

                // Report rendering with all panels collapsed
                this@shouldBeInvoicesOverviewPage.reportRendering("invoices-overview.loaded-collapsed")

                // Expand and verify details for each invoice
                staticItems[0].shouldHaveDetails(
                    actions = actions(hasEdit = true, hasMarkSent = false, hasMarkPaid = false),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Status" to paidStatus("Finalized"),
                        "Customer" to "Space Customer",
                        "Invoice Currency" to "EUR",
                        "Invoice Amount" to "EUR 140.00",
                        "Date Issued" to "10 Jan 3025",
                        "Due Date" to "14 Jan 3025",
                        "Date Sent" to "11 Jan 3025",
                        "Date Paid" to "6 Jan 3025",
                        "Applicable General Tax" to "VAT",
                        "Applicable General Tax Rate" to "20%"
                    ),
                    DetailsSectionSpec(
                        title = "Attachments",
                        content = SaDocumentsList.documentsValue("Invoice 1")
                    ),
                    DetailsSectionSpec(
                        title = "Additional Notes",
                        content = SaMarkdownOutput.markdownValue("Complex invoice with all attributes")
                    )
                )

                staticItems[1].shouldHaveDetails(
                    actions = actions(hasEdit = true, hasMarkSent = false, hasMarkPaid = false),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Status" to paidStatus("Finalized"),
                        "Customer" to "Space Customer",
                        "Invoice Amount" to "USD 60.00",
                        "Date Issued" to "9 Jan 3025",
                        "Due Date" to "13 Jan 3025",
                        "Date Sent" to "10 Jan 3025",
                        "Date Paid" to "7 Jan 3025"
                    ),
                    DetailsSectionSpec(
                        title = "Additional Notes",
                        content = SaMarkdownOutput.markdownValue("Important invoice notes")
                    )
                )

                staticItems[2].shouldHaveDetails(
                    actions = actions(hasEdit = true, hasMarkSent = false, hasMarkPaid = false),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Status" to paidStatus("Finalized"),
                        "Customer" to "Space Customer",
                        "Invoice Amount" to "USD 90.00",
                        "Date Issued" to "8 Jan 3025",
                        "Due Date" to "12 Jan 3025",
                        "Date Sent" to "9 Jan 3025",
                        "Date Paid" to "8 Jan 3025"
                    ),
                    DetailsSectionSpec(
                        title = "Attachments",
                        content = SaDocumentsList.documentsValue("Invoice 1", "Invoice 2")
                    )
                )

                staticItems[3].shouldHaveDetails(
                    actions = actions(hasEdit = true, hasMarkSent = false, hasMarkPaid = false),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Status" to paidStatus("Finalized"),
                        "Customer" to "Space Customer",
                        "Invoice Amount" to "USD 120.00",
                        "Date Issued" to "7 Jan 3025",
                        "Due Date" to "11 Jan 3025",
                        "Date Sent" to "8 Jan 3025",
                        "Date Paid" to "9 Jan 3025",
                        "Applicable General Tax" to "VAT",
                        "Applicable General Tax Rate" to "20%"
                    )
                )

                staticItems[4].shouldHaveDetails(
                    actions = actions(hasEdit = true, hasMarkSent = false, hasMarkPaid = false),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Status" to paidStatus("Finalized"),
                        "Customer" to "Space Customer",
                        "Invoice Currency" to "EUR",
                        "Invoice Amount" to "EUR 80.00",
                        "Date Issued" to "6 Jan 3025",
                        "Due Date" to "10 Jan 3025",
                        "Date Sent" to "7 Jan 3025",
                        "Date Paid" to "10 Jan 3025"
                    )
                )

                staticItems[5].shouldHaveDetails(
                    actions = actions(hasEdit = true, hasMarkSent = false, hasMarkPaid = false),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Status" to cancelledStatus("Cancelled"),
                        "Customer" to "Space Customer",
                        "Invoice Amount" to "USD 50.00",
                        "Date Issued" to "5 Jan 3025",
                        "Due Date" to "15 Jan 3025"
                    )
                )

                staticItems[6].shouldHaveDetails(
                    actions = actions(hasEdit = true, hasMarkSent = true, hasMarkPaid = false),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Status" to draftStatus("Draft"),
                        "Customer" to "Space Customer",
                        "Invoice Amount" to "USD 75.00",
                        "Date Issued" to "4 Jan 3025",
                        "Due Date" to "20 Jan 3025"
                    )
                )

                staticItems[7].shouldHaveDetails(
                    actions = actions(hasEdit = true, hasMarkSent = false, hasMarkPaid = true),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Status" to overdueStatus("Overdue"),
                        "Customer" to "Space Customer",
                        "Invoice Amount" to "USD 150.00",
                        "Date Issued" to "3 Jan 3025",
                        "Due Date" to "5 Jan 3025",
                        "Date Sent" to "4 Jan 3025"
                    )
                )

                staticItems[8].shouldHaveDetails(
                    actions = actions(hasEdit = true, hasMarkSent = false, hasMarkPaid = true),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Status" to sentStatus("Sent"),
                        "Customer" to "Space Customer",
                        "Invoice Amount" to "USD 200.00",
                        "Date Issued" to "2 Jan 3025",
                        "Due Date" to "30 Jan 3025",
                        "Date Sent" to "5 Jan 3025"
                    )
                )

                staticItems[9].shouldHaveDetails(
                    actions = actions(hasEdit = true, hasMarkSent = false, hasMarkPaid = false),
                    DetailsSectionSpec(
                        title = "General Information",
                        "Status" to paidStatus("Finalized"),
                        "Customer" to "Space Customer",
                        "Invoice Amount" to "USD 100.00",
                        "Date Issued" to "1 Jan 3025",
                        "Due Date" to "5 Jan 3025",
                        "Date Sent" to "3 Jan 3025",
                        "Date Paid" to "15 Jan 3025"
                    )
                )

                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }

            // Report rendering with all panels expanded
            reportRendering("invoices-overview.loaded-expanded")
        }
    }

    private fun actions(hasEdit: Boolean, hasMarkSent: Boolean, hasMarkPaid: Boolean): List<String> {
        val actions = mutableListOf<String>()
        if (hasEdit) actions.add(SaActionLink.editActionLinkValue())
        if (hasMarkSent) actions.add(SaActionLink.markAsSentActionLinkValue())
        if (hasMarkPaid) actions.add(SaActionLink.markAsPaidActionLinkValue())
        return actions
    }

    private fun paidStatus(label: String = "Finalized") = dataValues(SaStatusLabel.successStatusValue(), label)

    private fun sentStatus(label: String = "Sent") = dataValues(SaStatusLabel.pendingStatusValue(), label)

    private fun overdueStatus(label: String = "Overdue") = dataValues(SaStatusLabel.errorStatusValue(), label)

    private fun draftStatus(label: String = "Draft") = dataValues(SaStatusLabel.regularStatusValue(), label)

    private fun cancelledStatus(label: String = "Cancelled") = dataValues(SaStatusLabel.regularStatusValue(), label)

    @Test
    fun `should support pagination`(page: Page) {
        page.authenticateViaCookie(preconditionsPagination.fry)
        // Invoices are sorted by dateIssued descending (newest first)
        // Invoice 1 has date Jan 1 (newest)
        // Invoice 2 has date Jan 1 - 1 = Dec 31
        // ...
        // Invoice 15 has date Jan 1 - 14 = Dec 18 (oldest)
        val firstPageInvoices = listOf(
            "Invoice 1", "Invoice 2", "Invoice 3", "Invoice 4", "Invoice 5",
            "Invoice 6", "Invoice 7", "Invoice 8", "Invoice 9", "Invoice 10"
        )
        val secondPageInvoices = listOf(
            "Invoice 11", "Invoice 12", "Invoice 13", "Invoice 14", "Invoice 15"
        )
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveTitles(firstPageInvoices)
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(secondPageInvoices)
                paginator {
                    previous()
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(firstPageInvoices)
            }
        }
    }

    @Test
    fun `should support filtering by free text search`(page: Page) {
        page.authenticateViaCookie(preconditionsFiltering.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                // ensure data loaded
                shouldHaveDataSatisfying { items ->
                    items.map { it.title }.shouldContainAll("Office Invoice", "Travel Invoice", "Meals Invoice")
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
                shouldHaveTitles("Office Invoice")
                // ensure paginator updated
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }

            // Filter by customer name
            filterInput { fill("travel") }
            pageItems {
                shouldHaveTitles("Travel Invoice")
            }

            // Filter by notes
            filterInput { fill("urgent") }
            pageItems {
                shouldHaveTitles("Meals Invoice")
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
            val customer = customer(workspace = workspace, name = "Space Customer")
            val generalTax = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
            val document1 = document(workspace = workspace, name = "Invoice 1")
            val document2 = document(workspace = workspace, name = "Invoice 2")

            init {
                // 1. Paid invoice
                invoice(
                    customer = customer,
                    title = "Paid Invoice",
                    dateIssued = LocalDate.of(3025, 1, 1),
                    dueDate = LocalDate.of(3025, 1, 5),
                    dateSent = LocalDate.of(3025, 1, 3),
                    datePaid = LocalDate.of(3025, 1, 15),
                    amount = 10000,
                    status = InvoiceStatus.PAID
                )

                // 2. Sent invoice
                invoice(
                    customer = customer,
                    title = "Sent Invoice",
                    dateIssued = LocalDate.of(3025, 1, 2),
                    dueDate = LocalDate.of(3025, 1, 30),
                    dateSent = LocalDate.of(3025, 1, 5),
                    amount = 20000,
                    status = InvoiceStatus.SENT
                )

                // 3. Overdue invoice
                invoice(
                    customer = customer,
                    title = "Overdue Invoice",
                    dateIssued = LocalDate.of(3025, 1, 3),
                    dueDate = LocalDate.of(3025, 1, 5),
                    dateSent = LocalDate.of(3025, 1, 4),
                    amount = 15000,
                    status = InvoiceStatus.OVERDUE
                )

                // 4. Draft invoice
                invoice(
                    customer = customer,
                    title = "Draft Invoice",
                    dateIssued = LocalDate.of(3025, 1, 4),
                    dueDate = LocalDate.of(3025, 1, 20),
                    amount = 7500,
                    status = InvoiceStatus.DRAFT
                )

                // 5. Cancelled invoice
                invoice(
                    customer = customer,
                    title = "Cancelled Invoice",
                    dateIssued = LocalDate.of(3025, 1, 5),
                    dueDate = LocalDate.of(3025, 1, 15),
                    amount = 5000,
                    status = InvoiceStatus.CANCELLED
                )

                // 6. Foreign currency invoice
                invoice(
                    customer = customer,
                    title = "Foreign Currency Invoice",
                    dateIssued = LocalDate.of(3025, 1, 6),
                    dueDate = LocalDate.of(3025, 1, 10),
                    dateSent = LocalDate.of(3025, 1, 7),
                    datePaid = LocalDate.of(3025, 1, 10),
                    currency = "EUR",
                    amount = 8000,
                    status = InvoiceStatus.PAID
                )

                // 7. Invoice with tax
                invoice(
                    customer = customer,
                    title = "Invoice With Tax",
                    dateIssued = LocalDate.of(3025, 1, 7),
                    dueDate = LocalDate.of(3025, 1, 11),
                    dateSent = LocalDate.of(3025, 1, 8),
                    datePaid = LocalDate.of(3025, 1, 9),
                    amount = 12000,
                    generalTax = generalTax,
                    status = InvoiceStatus.PAID
                )

                // 8. Invoice with attachments
                invoice(
                    customer = customer,
                    title = "Invoice With Attachments",
                    dateIssued = LocalDate.of(3025, 1, 8),
                    dueDate = LocalDate.of(3025, 1, 12),
                    dateSent = LocalDate.of(3025, 1, 9),
                    datePaid = LocalDate.of(3025, 1, 8),
                    amount = 9000,
                    attachments = setOf(document1, document2),
                    status = InvoiceStatus.PAID
                )

                // 9. Invoice with notes
                invoice(
                    customer = customer,
                    title = "Invoice With Notes",
                    dateIssued = LocalDate.of(3025, 1, 9),
                    dueDate = LocalDate.of(3025, 1, 13),
                    dateSent = LocalDate.of(3025, 1, 10),
                    datePaid = LocalDate.of(3025, 1, 7),
                    amount = 6000,
                    notes = "Critical delivery invoice notes",
                    status = InvoiceStatus.PAID
                )

                // 10. Invoice with all attributes
                invoice(
                    customer = customer,
                    title = "Invoice With All Attributes",
                    dateIssued = LocalDate.of(3025, 1, 10),
                    dueDate = LocalDate.of(3025, 1, 14),
                    dateSent = LocalDate.of(3025, 1, 11),
                    datePaid = LocalDate.of(3025, 1, 6),
                    currency = "EUR",
                    amount = 14000,
                    generalTax = generalTax,
                    attachments = setOf(document1),
                    notes = "Planet Express invoice with all attributes",
                    status = InvoiceStatus.PAID
                )
            }
        }
    }

    private val preconditionsPagination by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val customer = customer(workspace = workspace)

            init {
                val baseDate = LocalDate.of(3025, 1, 1)
                (1..15).forEach { index ->
                    invoice(
                        customer = customer,
                        title = "Invoice $index",
                        dateIssued = baseDate.minusDays((index - 1).toLong()),
                        dueDate = baseDate.plusDays(10),
                        amount = 1000 * index.toLong(),
                        status = InvoiceStatus.DRAFT
                    )
                }
            }
        }
    }

    private val preconditionsFiltering by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val officeCustomer = customer(workspace = workspace, name = "Office")
            val travelCustomer = customer(workspace = workspace, name = "Slurm Corp")
            val mealsCustomer = customer(workspace = workspace, name = "Meals")
            val otherCustomer = customer(workspace = workspace, name = "Omicronians Inc")

            init {
                val baseDate = LocalDate.of(3025, 1, 1)
                invoice(
                    customer = officeCustomer,
                    title = "Office Invoice",
                    dateIssued = baseDate.minusDays(1),
                    dueDate = baseDate.plusDays(10),
                    amount = 10000,
                    status = InvoiceStatus.DRAFT
                )
                invoice(
                    customer = travelCustomer,
                    title = "Travel Invoice",
                    dateIssued = baseDate.minusDays(2),
                    dueDate = baseDate.plusDays(10),
                    amount = 5000,
                    status = InvoiceStatus.DRAFT
                )
                invoice(
                    customer = mealsCustomer,
                    title = "Meals Invoice",
                    dateIssued = baseDate.minusDays(3),
                    dueDate = baseDate.plusDays(10),
                    amount = 3000,
                    status = InvoiceStatus.DRAFT,
                    notes = "This is a Zoidberg emergency"
                )
                (1..10).forEach { index ->
                    invoice(
                        customer = otherCustomer,
                        title = "Other $index",
                        dateIssued = baseDate.minusDays(10L + index),
                        dueDate = baseDate.plusDays(10),
                        amount = 1000,
                        status = InvoiceStatus.DRAFT
                    )
                }
            }
        }
    }
}
