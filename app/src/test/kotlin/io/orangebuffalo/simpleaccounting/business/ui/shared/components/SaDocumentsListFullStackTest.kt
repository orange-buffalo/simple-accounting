package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.InvoicesOverviewPage.Companion.openInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaDocumentsList
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test

/**
 * Comprehensive full stack tests for DocumentsList component (SaDocumentsList).
 * Uses Invoice Overview panel as testing grounds.
 */
class SaDocumentsListFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display storage loading placeholder while storage status is loading`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val doc = document(workspace = workspace)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(doc),
                    title = "Slurm delivery invoice"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)

        page.withBlockedGqlApiResponse(
            "documentsStorageStatus",
            initiator = {
                page.openInvoicesOverviewPage {
                    pageItems {
                        shouldHaveItemSatisfying { it.title == "Slurm delivery invoice" }
                            .openDetails()
                    }
                }
            },
            blockedRequestSpec = {
                val documentsList = SaDocumentsList.singleton(page)
                documentsList {
                    shouldHaveStorageLoadingPlaceholder()
                    reportRendering("documents-list.storage-loading")
                }
            }
        )
    }

    @Test
    fun `should display error when storage is not configured`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = null)
                val workspace = workspace(owner = fry)
                val doc = document(workspace = workspace)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(doc),
                    title = "Slurm delivery invoice"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Slurm delivery invoice" }
                    .openDetails()
            }
        }

        SaDocumentsList.singleton(page).apply {
            shouldHaveStorageErrorMessage("Documents storage is not active")
            reportRendering("documents-list.storage-not-configured")
        }
    }

    @Test
    fun `should display error when storage is not active`(page: Page) {
        testDocumentsStorage.setStorageStatus(active = false)

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val doc = document(workspace = workspace)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(doc),
                    title = "Slurm delivery invoice"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Slurm delivery invoice" }
                    .openDetails()
            }
        }

        SaDocumentsList.singleton(page).apply {
            shouldHaveStorageErrorMessage("Documents storage is not active")
            reportRendering("documents-list.storage-not-active")
        }
    }

    @Test
    fun `should show documents loading state when documents are being fetched`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val doc = document(workspace = workspace, name = "slurm-invoice.pdf")
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(doc),
                    title = "Slurm delivery invoice"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)

        page.withBlockedApiResponse(
            "workspaces/**/documents*",
            initiator = {
                page.openInvoicesOverviewPage {
                    pageItems {
                        shouldHaveItemSatisfying { it.title == "Slurm delivery invoice" }
                            .openDetails()
                    }
                }
            },
            blockedRequestSpec = {
                val documentsList = SaDocumentsList.singleton(page)
                documentsList {
                    shouldHaveDocuments(SaDocumentsList.DocumentItem.Loading)
                    reportRendering("documents-list.documents-loading")
                }
            }
        )
    }

    @Test
    fun `should load and display single document`(page: Page) {
        val documentContent = "Slurm delivery receipt content".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val doc = document(
                    workspace = workspace,
                    name = "slurm-delivery-receipt.pdf",
                    storageId = TestDocumentsStorage.STORAGE_ID,
                    storageLocation = "slurm-receipt-location",
                    sizeInBytes = documentContent.size.toLong(),
                    timeUploaded = MOCK_TIME
                )
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(doc),
                    title = "Slurm delivery invoice"
                )
            }
        }

        testDocumentsStorage.mockDocumentContent("slurm-receipt-location", documentContent)

        page.authenticateViaCookie(preconditions.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Slurm delivery invoice" }
                    .openDetails()
            }
        }

        val documentsList = SaDocumentsList.singleton(page)
        documentsList {
            shouldHaveDocuments(SaDocumentsList.DocumentItem.Ready("slurm-delivery-receipt.pdf", "(30 byte)"))
            reportRendering("documents-list.single-document")
        }
    }

    @Test
    fun `should load and display multiple documents sorted alphabetically`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "zoidberg-contract.pdf",
                            storageId = TestDocumentsStorage.STORAGE_ID,
                            storageLocation = "zoidberg-location"
                        ),
                        document(
                            workspace = workspace,
                            name = "bender-receipt.jpg",
                            storageId = TestDocumentsStorage.STORAGE_ID,
                            storageLocation = "bender-location"
                        ),
                        document(
                            workspace = workspace,
                            name = "leela-invoice.docx",
                            storageId = TestDocumentsStorage.STORAGE_ID,
                            storageLocation = "leela-location"
                        )
                    ),
                    title = "Planet Express delivery"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Planet Express delivery" }
                    .openDetails()
            }
        }

        val documentsList = SaDocumentsList.singleton(page)
        documentsList {
            // Documents are sorted alphabetically by name; no sizeInBytes set so size = null
            shouldHaveDocuments(
                SaDocumentsList.DocumentItem.Ready("bender-receipt.jpg"),
                SaDocumentsList.DocumentItem.Ready("leela-invoice.docx"),
                SaDocumentsList.DocumentItem.Ready("zoidberg-contract.pdf")
            )
            reportRendering("documents-list.multiple-documents-sorted")
        }
    }

    @Test
    fun `should download document with correct content`(page: Page) {
        val documentContent = "Good news, everyone! Slurm delivery confirmed.".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val doc = document(
                    workspace = workspace,
                    name = "slurm-receipt.pdf",
                    storageId = TestDocumentsStorage.STORAGE_ID,
                    storageLocation = "download-test-location",
                    sizeInBytes = documentContent.size.toLong(),
                    timeUploaded = MOCK_TIME
                )
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(doc),
                    title = "Slurm delivery invoice"
                )
            }
        }

        testDocumentsStorage.mockDocumentContent("download-test-location", documentContent)

        page.authenticateViaCookie(preconditions.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Slurm delivery invoice" }
                    .openDetails()
            }
        }

        val documentsList = SaDocumentsList.singleton(page)
        documentsList {
            shouldHaveDocuments(SaDocumentsList.DocumentItem.Ready("slurm-receipt.pdf", "(46 byte)"))
            val downloadedContent = downloadDocument("slurm-receipt.pdf")
            downloadedContent.shouldBe(documentContent)
        }
    }
}
