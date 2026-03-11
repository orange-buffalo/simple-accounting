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
    fun `should display storage loading placeholder while download storages query is loading`(page: Page) {
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
            "downloadDocumentStorages",
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
    fun `should display error when document has unsupported storage`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val doc = document(
                    workspace = workspace,
                    storageId = "unavailable-storage",
                    storageLocation = "some-location"
                )
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
            shouldHaveStorageErrorMessage("Some uploaded documents cannot be processed")
            reportRendering("documents-list.unsupported-storage")
        }
    }

    @Test
    fun `should display error when any document has unsupported storage in a mixed list`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "supported-doc.pdf",
                            storageId = TestDocumentsStorage.STORAGE_ID,
                            storageLocation = "supported-location"
                        ),
                        document(
                            workspace = workspace,
                            name = "unsupported-doc.pdf",
                            storageId = "unavailable-storage",
                            storageLocation = "unsupported-location"
                        )
                    ),
                    title = "Planet Express mixed delivery"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Planet Express mixed delivery" }
                    .openDetails()
            }
        }

        SaDocumentsList.singleton(page).apply {
            shouldHaveStorageErrorMessage("Some uploaded documents cannot be processed")
            reportRendering("documents-list.mixed-unsupported-storage")
        }
    }

    @Test
    fun `should display documents when storage is not configured in profile but documents use supported storage`(page: Page) {
        val documentContent = "Slurm receipt from the past".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = null)
                val workspace = workspace(owner = fry)
                val doc = document(
                    workspace = workspace,
                    name = "past-receipt.pdf",
                    storageId = TestDocumentsStorage.STORAGE_ID,
                    storageLocation = "past-receipt-location",
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

        testDocumentsStorage.mockDocumentContent("past-receipt-location", documentContent)

        page.authenticateViaCookie(preconditions.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Slurm delivery invoice" }
                    .openDetails()
            }
        }

        val documentsList = SaDocumentsList.singleton(page)
        documentsList {
            shouldHaveDocuments(SaDocumentsList.DocumentItem.Ready("past-receipt.pdf", "(27 byte)"))
            reportRendering("documents-list.no-profile-storage-but-supported")
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
    fun `should render document size in bytes, kilobytes, and megabytes`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "bender-photo.jpg",
                            storageId = TestDocumentsStorage.STORAGE_ID,
                            storageLocation = "bender-photo-location",
                            sizeInBytes = 500,
                        ),
                        document(
                            workspace = workspace,
                            name = "leela-scan.pdf",
                            storageId = TestDocumentsStorage.STORAGE_ID,
                            storageLocation = "leela-scan-location",
                            sizeInBytes = 2048,
                        ),
                        document(
                            workspace = workspace,
                            name = "zoidberg-report.docx",
                            storageId = TestDocumentsStorage.STORAGE_ID,
                            storageLocation = "zoidberg-report-location",
                            sizeInBytes = 1024L * 1024 * 2,
                        ),
                    ),
                    title = "Planet Express size test"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Planet Express size test" }
                    .openDetails()
            }
        }

        val documentsList = SaDocumentsList.singleton(page)
        documentsList {
            shouldHaveDocuments(
                SaDocumentsList.DocumentItem.Ready("bender-photo.jpg", "(500 byte)"),
                SaDocumentsList.DocumentItem.Ready("leela-scan.pdf", "(2 kB)"),
                SaDocumentsList.DocumentItem.Ready("zoidberg-report.docx", "(2 MB)"),
            )
            reportRendering("documents-list.size-rendering")
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
