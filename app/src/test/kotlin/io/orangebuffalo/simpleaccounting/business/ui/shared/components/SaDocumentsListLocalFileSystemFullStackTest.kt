package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.InvoicesOverviewPage.Companion.openInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaDocumentsList
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes

/**
 * Full stack tests for DocumentsList component (SaDocumentsList) with LocalFileSystemDocumentsStorage integration.
 * Unlike [SaDocumentsListGoogleDriveFullStackTest] that uses Google Drive, this test verifies the real local file
 * system storage integration where documents are read from a temporary directory.
 * Uses Invoice Overview panel as testing grounds.
 *
 * See also [SaDocumentsUploadLocalFileSystemFullStackTest] for local file system upload flow tests.
 */
class SaDocumentsListLocalFileSystemFullStackTest : SaFullStackTestBase() {

    @TempDir
    private lateinit var tempDir: Path

    @BeforeEach
    fun setupLocalFsStorage() {
        whenever(localFsStorageProperties.baseDirectory) doReturn tempDir
    }

    @Test
    fun `should load and display single document from local file system`(page: Page) {
        val documentContent = "Spaceship fuel receipt from Mars".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "local-fs")
                val workspace = workspace(owner = fry)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "fuel-receipt.pdf",
                            storageId = "local-fs",
                            storageLocation = "${workspace.id}/fuel-receipt.pdf",
                            sizeInBytes = documentContent.size.toLong(),
                            timeUploaded = MOCK_TIME,
                            mimeType = "application/pdf"
                        )
                    ),
                    title = "Spaceship fuel delivery"
                )
            }
        }

        val storedFile = tempDir.resolve("${preconditions.workspace.id}/fuel-receipt.pdf")
        Files.createDirectories(storedFile.parent)
        storedFile.writeBytes(documentContent)

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
    fun `should load and display multiple documents from local file system sorted alphabetically`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "local-fs")
                val workspace = workspace(owner = fry)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "zoidberg-contract.pdf",
                            storageId = "local-fs",
                            storageLocation = "${workspace.id}/zoidberg-contract.pdf",
                        ),
                        document(
                            workspace = workspace,
                            name = "bender-receipt.jpg",
                            storageId = "local-fs",
                            storageLocation = "${workspace.id}/bender-receipt.jpg",
                        ),
                        document(
                            workspace = workspace,
                            name = "leela-invoice.docx",
                            storageId = "local-fs",
                            storageLocation = "${workspace.id}/leela-invoice.docx",
                        )
                    ),
                    title = "Planet Express delivery"
                )
            }
        }

        val workspaceDir = tempDir.resolve("${preconditions.workspace.id}")
        Files.createDirectories(workspaceDir)
        workspaceDir.resolve("zoidberg-contract.pdf").writeBytes("Zoidberg contract content".toByteArray())
        workspaceDir.resolve("bender-receipt.jpg").writeBytes("Bender receipt content".toByteArray())
        workspaceDir.resolve("leela-invoice.docx").writeBytes("Leela invoice content".toByteArray())

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
    fun `should download document from local file system with correct content`(page: Page) {
        val documentContent = "Good news, everyone! Delivery contract for Omicron Persei 8".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "local-fs")
                val workspace = workspace(owner = fry)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "delivery-contract.pdf",
                            storageId = "local-fs",
                            storageLocation = "${workspace.id}/delivery-contract.pdf",
                            sizeInBytes = documentContent.size.toLong(),
                            timeUploaded = MOCK_TIME,
                            mimeType = "application/pdf"
                        )
                    ),
                    title = "Omicron Persei 8 delivery"
                )
            }
        }

        val storedFile = tempDir.resolve("${preconditions.workspace.id}/delivery-contract.pdf")
        Files.createDirectories(storedFile.parent)
        storedFile.writeBytes(documentContent)

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
