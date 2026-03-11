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
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
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

    private val documentContent = "Dark matter delivery receipt from Omicron Persei 8".toByteArray()

    @Test
    fun `should download document via workspace access token when using test storage`(page: Page) {
        val testData = createTestData(documentsStorage = TestDocumentsStorage.STORAGE_ID)
        verifyDocumentUploadAndDownloadByWorkspaceToken(page, testData)
    }

    @Test
    fun `should download document via workspace access token when using Google Drive`(page: Page) {
        val testData = createTestData(documentsStorage = "google-drive") {
            save(GoogleDriveStorageIntegration(userId = it.id!!, folderId = "root-folder-id"))
        }

        val accessToken = GoogleOAuthMocks.token()
            .enqueue()
            .persist(testData.fry)

        GoogleDriveApiMocks.mockFindFile(
            fileId = "root-folder-id",
            fileName = "simple-accounting",
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockFindFolder(
            parentFolderId = "root-folder-id",
            folderName = testData.workspace.id.toString(),
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

        verifyDocumentUploadAndDownloadByWorkspaceToken(page, testData)
    }

    private fun createTestData(
        documentsStorage: String,
        additionalUserSetup: EntitiesFactory.(PlatformUser) -> Unit = {},
    ) = preconditions {
        val fry = platformUser(userName = "Fry", documentsStorage = documentsStorage).also {
            additionalUserSetup(it)
        }
        val workspace = workspace(owner = fry)
        val invoice = invoice(
            customer = customer(workspace = workspace, name = "Mom"),
            title = INVOICE_TITLE,
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
        TestData(fry = fry, workspace = workspace, invoiceId = invoice.id!!, workspaceToken = workspaceToken)
    }

    private fun verifyDocumentUploadAndDownloadByWorkspaceToken(page: Page, testData: TestData) {
        val testFile = createTestFile("dark-matter-receipt.pdf", documentContent)

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoiceId}/edit")
        page.shouldBeEditInvoicePage {
            documentsUpload {
                selectFileForUpload(testFile)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile.fileName.toString(), DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument,
                )
            }
            saveButton.click()
        }
        page.shouldBeInvoicesOverviewPage()

        page.navigate("/login-by-link/${testData.workspaceToken}")
        page.shouldBeDashboardPage()

        page.shouldHaveSideMenu().clickInvoices()
        page.shouldBeInvoicesOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == INVOICE_TITLE }
                    .openDetails()
            }
        }

        SaDocumentsList.singleton(page).apply {
            val downloadedContent = downloadDocument(testFile.fileName.toString())
            downloadedContent.shouldBe(documentContent)
        }
    }

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = Files.createTempFile(tempDir, "test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }

    private data class TestData(
        val fry: PlatformUser,
        val workspace: Workspace,
        val invoiceId: Long,
        val workspaceToken: String,
    )

    companion object {
        private const val INVOICE_TITLE = "Dark matter shipment"
    }
}
