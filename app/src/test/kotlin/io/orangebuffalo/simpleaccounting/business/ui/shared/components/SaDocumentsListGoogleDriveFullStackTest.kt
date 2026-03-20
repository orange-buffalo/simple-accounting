package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.GoogleDriveStorageIntegration
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.InvoicesOverviewPage.Companion.openInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleDriveApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleOAuthMocks
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaDocumentsList
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.junit.jupiter.api.Test

/**
 * Full stack tests for DocumentsList component (SaDocumentsList) with Google Drive document storage integration.
 * Unlike [SaDocumentsListFullStackTest] that uses a test storage, this test verifies the real Google Drive
 * integration with only the external Google APIs being mocked via WireMock.
 * Uses Invoice Overview panel as testing grounds.
 *
 * See also [SaDocumentsUploadGoogleDriveFullStackTest] for Google Drive upload flow tests.
 */
class SaDocumentsListGoogleDriveFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should load and display single document from Google Drive`(page: Page) {
        val documentContent = "Spaceship fuel receipt from Mars".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "google-drive").also {
                    save(
                        GoogleDriveStorageIntegration(
                            userId = it.id!!,
                            folderId = "root-folder-id",
                        )
                    )
                }
                val workspace = workspace(owner = fry)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "fuel-receipt.pdf",
                            storageId = "google-drive",
                            storageLocation = "gdrive-file-id-1",
                            sizeInBytes = documentContent.size.toLong(),
                            timeUploaded = MOCK_TIME,
                            mimeType = "application/pdf"
                        )
                    ),
                    title = "Spaceship fuel delivery"
                )
            }
        }

        val accessToken = GoogleOAuthMocks.token()
            .enqueue()
            .persist(preconditions.fry)

        GoogleDriveApiMocks.mockFindFile(
            fileId = "root-folder-id",
            fileName = "simple-accounting",
            expectedAuthToken = accessToken,
        )

        page.authenticateViaCookie(preconditions.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Spaceship fuel delivery" }
                    .openDetails()
            }
        }

        val documentsList = SaDocumentsList.singleton(page)
        documentsList {
            shouldHaveDocuments(SaDocumentsList.DocumentItem.Ready("fuel-receipt.pdf", "(32 byte)"))
        }
    }

    @Test
    fun `should load and display multiple documents from Google Drive sorted alphabetically`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "google-drive").also {
                    save(
                        GoogleDriveStorageIntegration(
                            userId = it.id!!,
                            folderId = "root-folder-id",
                        )
                    )
                }
                val workspace = workspace(owner = fry)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "zoidberg-contract.pdf",
                            storageId = "google-drive",
                            storageLocation = "gdrive-file-id-z",
                        ),
                        document(
                            workspace = workspace,
                            name = "bender-receipt.jpg",
                            storageId = "google-drive",
                            storageLocation = "gdrive-file-id-b",
                        ),
                        document(
                            workspace = workspace,
                            name = "leela-invoice.docx",
                            storageId = "google-drive",
                            storageLocation = "gdrive-file-id-l",
                        )
                    ),
                    title = "Planet Express delivery"
                )
            }
        }

        val accessToken = GoogleOAuthMocks.token()
            .enqueue()
            .persist(preconditions.fry)

        GoogleDriveApiMocks.mockFindFile(
            fileId = "root-folder-id",
            fileName = "simple-accounting",
            expectedAuthToken = accessToken,
        )

        page.authenticateViaCookie(preconditions.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Planet Express delivery" }
                    .openDetails()
            }
        }

        val documentsList = SaDocumentsList.singleton(page)
        documentsList {
            shouldHaveDocuments(
                SaDocumentsList.DocumentItem.Ready("bender-receipt.jpg"),
                SaDocumentsList.DocumentItem.Ready("leela-invoice.docx"),
                SaDocumentsList.DocumentItem.Ready("zoidberg-contract.pdf")
            )
        }
    }

    @Test
    fun `should download document from Google Drive with correct content`(page: Page) {
        val documentContent = "Good news, everyone! Delivery contract for Omicron Persei 8".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "google-drive").also {
                    save(
                        GoogleDriveStorageIntegration(
                            userId = it.id!!,
                            folderId = "root-folder-id",
                        )
                    )
                }
                val workspace = workspace(owner = fry)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "delivery-contract.pdf",
                            storageId = "google-drive",
                            storageLocation = "gdrive-existing-file-id",
                            sizeInBytes = documentContent.size.toLong(),
                            timeUploaded = MOCK_TIME,
                            mimeType = "application/pdf"
                        )
                    ),
                    title = "Omicron Persei 8 delivery"
                )
            }
        }

        val accessToken = GoogleOAuthMocks.token()
            .enqueue()
            .persist(preconditions.fry)

        GoogleDriveApiMocks.mockFindFile(
            fileId = "root-folder-id",
            fileName = "simple-accounting",
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockDownloadFile(
            fileId = "gdrive-existing-file-id",
            content = documentContent,
            expectedAuthToken = accessToken,
        )

        page.authenticateViaCookie(preconditions.fry)
        page.openInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Omicron Persei 8 delivery" }
                    .openDetails()
            }
        }

        val documentsList = SaDocumentsList.singleton(page)
        documentsList {
            shouldHaveDocuments(SaDocumentsList.DocumentItem.Ready("delivery-contract.pdf", "(59 byte)"))
            val downloadedContent = downloadDocument("delivery-contract.pdf")
            downloadedContent.shouldBe(documentContent)
        }
    }
}
