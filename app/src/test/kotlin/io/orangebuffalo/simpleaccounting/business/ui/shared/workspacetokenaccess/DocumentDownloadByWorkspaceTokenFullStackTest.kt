package io.orangebuffalo.simpleaccounting.business.ui.shared.workspacetokenaccess

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.GoogleDriveStorageIntegration
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.EditInvoicePage.Companion.shouldBeEditInvoicePage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.InvoicesOverviewPage.Companion.shouldBeInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleDriveApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleOAuthMocks
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaDocumentsList
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import kotlin.io.path.writeBytes

/**
 * Acceptance tests that verify document download by workspace access token for documents
 * uploaded by a regular user. Covers both test storage and Google Drive storage.
 */
class DocumentDownloadByWorkspaceTokenFullStackTest : SaFullStackTestBase() {

    @TempDir
    private lateinit var tempDir: Path

    @Test
    fun `should download document via workspace access token when using test storage`(page: Page) {
        val documentContent = "Dark matter delivery receipt from Omicron Persei 8".toByteArray()
        val testPreconditions = preconditions {
            val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
            val workspace = workspace(owner = fry)
            val customer = customer(workspace = workspace, name = "Mom")
            val invoice = invoice(
                customer = customer,
                title = "Dark matter shipment",
                dateIssued = LocalDate.of(3025, 1, 1),
                dueDate = LocalDate.of(3025, 2, 1),
                currency = "USD",
                amount = 50000,
                status = InvoiceStatus.DRAFT,
            )
            val workspaceToken = workspaceAccessToken(
                workspace = workspace,
                token = "planet-express-token",
                validTill = Instant.parse("9999-12-31T23:59:59Z"),
                timeCreated = MOCK_TIME,
            ).token
            object {
                val fry = fry
                val invoice = invoice
                val workspaceToken = workspaceToken
            }
        }

        val testFile = createTestFile("dark-matter-receipt.pdf", documentContent)

        uploadDocumentAndDownloadByWorkspaceToken(
            page = page,
            user = testPreconditions.fry,
            invoiceId = testPreconditions.invoice.id!!,
            invoiceTitle = "Dark matter shipment",
            workspaceToken = testPreconditions.workspaceToken,
            uploadFile = testFile,
            expectedContent = documentContent,
        )
    }

    @Test
    fun `should download document via workspace access token when using Google Drive`(page: Page) {
        val documentContent = "Slurm supplies order for Planet Express".toByteArray()
        val testPreconditions = preconditions {
            val fry = platformUser(userName = "Fry", documentsStorage = "google-drive").also {
                save(
                    GoogleDriveStorageIntegration(
                        userId = it.id!!,
                        folderId = "root-folder-id",
                    )
                )
            }
            val workspace = workspace(owner = fry)
            val customer = customer(workspace = workspace, name = "Mom")
            val invoice = invoice(
                customer = customer,
                title = "Slurm supplies order",
                dateIssued = LocalDate.of(3025, 1, 1),
                dueDate = LocalDate.of(3025, 2, 1),
                currency = "USD",
                amount = 30000,
                status = InvoiceStatus.DRAFT,
            )
            val workspaceToken = workspaceAccessToken(
                workspace = workspace,
                token = "planet-express-token",
                validTill = Instant.parse("9999-12-31T23:59:59Z"),
                timeCreated = MOCK_TIME,
            ).token
            object {
                val fry = fry
                val workspace = workspace
                val invoice = invoice
                val workspaceToken = workspaceToken
            }
        }

        val accessToken = GoogleOAuthMocks.token()
            .enqueue()
            .persist(testPreconditions.fry)

        GoogleDriveApiMocks.mockFindFile(
            fileId = "root-folder-id",
            fileName = "simple-accounting",
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockFindFolder(
            parentFolderId = "root-folder-id",
            folderName = testPreconditions.workspace.id.toString(),
            responseFolderId = "workspace-folder-id",
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockUploadFile(
            responseId = "gdrive-uploaded-file-id",
            responseSize = documentContent.size.toLong(),
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockDownloadFile(
            fileId = "gdrive-uploaded-file-id",
            content = documentContent,
            expectedAuthToken = accessToken,
        )

        val testFile = createTestFile("slurm-order.pdf", documentContent)

        uploadDocumentAndDownloadByWorkspaceToken(
            page = page,
            user = testPreconditions.fry,
            invoiceId = testPreconditions.invoice.id!!,
            invoiceTitle = "Slurm supplies order",
            workspaceToken = testPreconditions.workspaceToken,
            uploadFile = testFile,
            expectedContent = documentContent,
        )
    }

    private fun uploadDocumentAndDownloadByWorkspaceToken(
        page: Page,
        user: PlatformUser,
        invoiceId: Long,
        invoiceTitle: String,
        workspaceToken: String,
        uploadFile: Path,
        expectedContent: ByteArray,
    ) {
        page.authenticateViaCookie(user)
        page.navigate("/invoices/$invoiceId/edit")
        page.shouldBeEditInvoicePage {
            documentsUpload {
                selectFileForUpload(uploadFile)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(uploadFile.fileName.toString(), DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument,
                )
            }
            saveButton.click()
        }
        page.shouldBeInvoicesOverviewPage()

        page.navigate("/login-by-link/$workspaceToken")
        page.shouldBeDashboardPage()

        page.shouldHaveSideMenu().clickInvoices()
        page.shouldBeInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == invoiceTitle }
                    .openDetails()
            }
        }

        SaDocumentsList.singleton(page).apply {
            val downloadedContent = downloadDocument(uploadFile.fileName.toString())
            downloadedContent.shouldBe(expectedContent)
        }
    }

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = Files.createTempFile(tempDir, "test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }
}
