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
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test
import java.time.Instant

class DocumentsOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display documents with all possible attribute variations`(page: Page) {
        page.authenticateViaCookie(preconditionsAllStates.fry)

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
                        title = "Unused Receipt.pdf",
                        primaryAttributes = primaryAttributes(
                            "Mar 28, 1999, 11:01 PM",
                            "Internal System"
                        ),
                        middleColumnContent = SaStatusLabel.pendingStatusValue() + "Unused",
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Single Usage Doc.pdf",
                        primaryAttributes = primaryAttributes(
                            "Jan 15, 3025, 10:30 AM",
                            "Internal System"
                        ),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Multi Usage Doc.pdf",
                        primaryAttributes = primaryAttributes(
                            "Jan 14, 3025, 9:00 AM",
                            "Internal System"
                        ),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Google Drive Doc.pdf",
                        primaryAttributes = primaryAttributes(
                            "Jan 13, 3025, 8:00 AM",
                            "Google Drive"
                        ),
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

    @Test
    fun `should navigate to edit expense page from usage link`(page: Page) {
        page.authenticateViaCookie(preconditionsNavigation.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Expense Receipt.pdf")
                val middleColumn = staticItems[0].locator(".overview-item__middle-column")
                middleColumn.locator("a").first().click()
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
        page.authenticateViaCookie(preconditionsNavigationIncome.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Income Receipt.pdf")
                val middleColumn = staticItems[0].locator(".overview-item__middle-column")
                middleColumn.locator("a").first().click()
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
        page.authenticateViaCookie(preconditionsNavigationInvoice.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Invoice Attachment.pdf")
                val middleColumn = staticItems[0].locator(".overview-item__middle-column")
                middleColumn.locator("a").first().click()
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
        page.authenticateViaCookie(preconditionsNavigationTaxPayment.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Tax Payment Receipt.pdf")
                val middleColumn = staticItems[0].locator(".overview-item__middle-column")
                middleColumn.locator("a").first().click()
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
        page.authenticateViaCookie(preconditionsPagination.fry)
        val firstPageDocuments = (1..10).map { "Document $it.pdf" }
        val secondPageDocuments = (11..15).map { "Document $it.pdf" }

        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles(firstPageDocuments)
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(secondPageDocuments)
                paginator {
                    previous()
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(firstPageDocuments)
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

            init {
                // 1. Unused document (no usages)
                document(
                    workspace = workspace,
                    name = "Unused Receipt.pdf",
                )

                // 2. Document with single usage (expense)
                val singleUsageDoc = document(
                    workspace = workspace,
                    name = "Single Usage Doc.pdf",
                    timeUploaded = Instant.parse("3025-01-15T10:30:00Z"),
                )
                expense(
                    workspace = workspace,
                    title = "Slurm supplies",
                    attachments = setOf(singleUsageDoc),
                )

                // 3. Document with multiple usages (expense + income)
                val multiUsageDoc = document(
                    workspace = workspace,
                    name = "Multi Usage Doc.pdf",
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

                // 4. Document stored in Google Drive
                document(
                    workspace = workspace,
                    name = "Google Drive Doc.pdf",
                    timeUploaded = Instant.parse("3025-01-13T08:00:00Z"),
                    storageId = "google-drive",
                )
            }
        }
    }

    private val preconditionsNavigation by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)

            init {
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

    private val preconditionsNavigationIncome by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)

            init {
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

    private val preconditionsNavigationInvoice by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)

            init {
                val doc = document(
                    workspace = workspace,
                    name = "Invoice Attachment.pdf",
                )
                invoice(
                    title = "Delivery to Omicron Persei 8",
                    attachments = setOf(doc),
                )
            }
        }
    }

    private val preconditionsNavigationTaxPayment by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)

            init {
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

    private val preconditionsPagination by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)

            init {
                (1..15).forEach { index ->
                    document(
                        workspace = workspace,
                        name = "Document $index.pdf",
                        timeUploaded = Instant.parse("3025-01-01T00:00:00Z").minusSeconds(index.toLong()),
                    )
                }
            }
        }
    }
}
