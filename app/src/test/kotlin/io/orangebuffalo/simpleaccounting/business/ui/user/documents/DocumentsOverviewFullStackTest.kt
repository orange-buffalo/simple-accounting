package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.DocumentsOverviewPage.Companion.openDocumentsOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.DocumentsOverviewPage.Companion.shouldBeDocumentsOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.EditIncomePage.Companion.shouldBeEditIncomePage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.EditIncomeTaxPaymentPage.Companion.shouldBeEditIncomeTaxPaymentPage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.EditInvoicePage.Companion.shouldBeEditInvoicePage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.primaryAttribute
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItemData
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaIconType
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.dataValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test
import java.time.Instant

class DocumentsOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display documents with all possible attribute variations`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    isAdmin = false,
                    activated = true,
                    documentsStorage = "noop"
                )

                init {
                    val workspace = workspace(owner = fry)

                    document(
                        workspace = workspace,
                        name = "Unused Receipt.pdf",
                        createdAt = MOCK_TIME,
                    )

                    val singleUsageDoc = document(
                        workspace = workspace,
                        name = "Single Usage Doc.pdf",
                        createdAt = MOCK_TIME.plusSeconds(1),
                        timeUploaded = Instant.parse("3025-01-15T21:30:00Z"),
                    )
                    expense(
                        workspace = workspace,
                        title = "Slurm supplies",
                        attachments = setOf(singleUsageDoc),
                    )

                    val multiUsageDoc = document(
                        workspace = workspace,
                        name = "Multi Usage Doc.pdf",
                        createdAt = MOCK_TIME.plusSeconds(2),
                        timeUploaded = Instant.parse("3025-01-14T09:00:00Z"),
                    )
                    expense(
                        workspace = workspace,
                        title = "Robot oil",
                        attachments = setOf(multiUsageDoc),
                    )
                    income(
                        workspace = workspace,
                        title = "Delivery commission",
                        attachments = setOf(multiUsageDoc),
                    )

                    document(
                        workspace = workspace,
                        name = "Google Drive Doc.pdf",
                        createdAt = MOCK_TIME.plusSeconds(3),
                        timeUploaded = Instant.parse("3025-01-13T08:00:00Z"),
                        storageId = "google-drive",
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)

        page.withBlockedGqlApiResponse(
            "documentsPage",
            initiator = {
                page.openDocumentsOverviewPage { }
            },
            blockedRequestSpec = {
                page.shouldBeDocumentsOverviewPage {
                    pageItems.shouldHaveLoadingIndicatorVisible()
                    reportRendering("documents-overview.loading")
                }
            }
        )

        page.shouldBeDocumentsOverviewPage {
            pageItems {
                shouldHaveExactData(
                    SaOverviewItemData(
                        title = "Google Drive Doc.pdf",
                        primaryAttributes = primaryAttributes(
                            "13 Jan 3025, 8:00 am",
                            "Google Drive"
                        ),
                        middleColumnContent = unusedStatus(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Multi Usage Doc.pdf",
                        primaryAttributes = primaryAttributes(
                            "14 Jan 3025, 9:00 am",
                            "Unknown"
                        ),
                        middleColumnContent = "Robot oilDelivery commission",
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Single Usage Doc.pdf",
                        primaryAttributes = primaryAttributes(
                            "15 Jan 3025, 9:30 pm",
                            "Unknown"
                        ),
                        middleColumnContent = "Slurm supplies",
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Unused Receipt.pdf",
                        primaryAttributes = primaryAttributes(
                            "28 Mar 1999, 11:01 pm",
                            "Unknown"
                        ),
                        middleColumnContent = unusedStatus(),
                        hasDetails = false,
                    ),
                )

                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
            reportRendering("documents-overview.loaded")
        }
    }

    private fun primaryAttributes(dateTime: String, storage: String) = listOf(
        primaryAttribute(SaIconType.CALENDAR, text = dateTime),
        primaryAttribute(SaIconType.UPLOAD, text = storage),
    )

    private fun unusedStatus() = dataValues(SaStatusLabel.pendingStatusValue(), "Unused")

    @Test
    fun `should navigate to edit expense page from usage link`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    val doc = document(
                        workspace = workspace,
                        name = "Expense Receipt.pdf",
                    )
                    expense(
                        workspace = workspace,
                        title = "Slurm supplies",
                        attachments = setOf(doc),
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Expense Receipt.pdf")
                staticItems[0].clickMiddleColumnLink("Slurm supplies")
            }
        }

        page.shouldBeEditExpensePage {
            title {
                input.shouldHaveValue("Slurm supplies")
            }
        }
    }

    @Test
    fun `should navigate to edit income page from usage link`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    val doc = document(
                        workspace = workspace,
                        name = "Income Receipt.pdf",
                    )
                    income(
                        workspace = workspace,
                        title = "Delivery commission",
                        attachments = setOf(doc),
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Income Receipt.pdf")
                staticItems[0].clickMiddleColumnLink("Delivery commission")
            }
        }

        page.shouldBeEditIncomePage {
            title {
                input.shouldHaveValue("Delivery commission")
            }
        }
    }

    @Test
    fun `should navigate to edit invoice page from usage link`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    val doc = document(
                        workspace = workspace,
                        name = "Invoice Attachment.pdf",
                    )
                    val customer = customer(workspace = workspace)
                    invoice(
                        customer = customer,
                        title = "Delivery to Omicron Persei 8",
                        attachments = setOf(doc),
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Invoice Attachment.pdf")
                staticItems[0].clickMiddleColumnLink("Delivery to Omicron Persei 8")
            }
        }

        page.shouldBeEditInvoicePage {
            title {
                input.shouldHaveValue("Delivery to Omicron Persei 8")
            }
        }
    }

    @Test
    fun `should navigate to edit income tax payment page from usage link`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    val doc = document(
                        workspace = workspace,
                        name = "Tax Payment Receipt.pdf",
                    )
                    incomeTaxPayment(
                        workspace = workspace,
                        title = "Q1 Corporate Tax",
                        attachments = setOf(doc),
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Tax Payment Receipt.pdf")
                staticItems[0].clickMiddleColumnLink("Q1 Corporate Tax")
            }
        }

        page.shouldBeEditIncomeTaxPaymentPage {
            title {
                input.shouldHaveValue("Q1 Corporate Tax")
            }
        }
    }

    @Test
    fun `should support pagination`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    (1..15).forEach { index ->
                        document(
                            workspace = workspace,
                            name = "Document $index.pdf",
                            createdAt = MOCK_TIME.plusSeconds(index.toLong()),
                        )
                    }
                }
            }
        }

        val firstPageDocuments = (15 downTo 6).map { "Document $it.pdf" }
        val secondPageDocuments = (5 downTo 1).map { "Document $it.pdf" }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/documents")
        page.shouldBeDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles(firstPageDocuments)
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                }
                shouldHaveTitles(secondPageDocuments)
                paginator {
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                    previous()
                }
                shouldHaveTitles(firstPageDocuments)
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
            }
        }
    }
}
