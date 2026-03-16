package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.GoogleDriveStorageIntegration
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleDriveApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleOAuthMocks
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.writeBytes

private val MOCK_EPOCH_MILLIS = MOCK_TIME.toEpochMilli()

/**
 * Full stack tests for DocumentsUpload component (SaDocumentsUpload) with Google Drive document storage integration.
 * Unlike [SaDocumentsUploadFullStackTest] that uses a test storage, this test verifies the real Google Drive
 * integration with only the external Google APIs being mocked via WireMock.
 * Uses Edit Expense page as testing grounds.
 *
 * See also [io.orangebuffalo.simpleaccounting.business.ui.user.profile.UserProfileGoogleDriveDocumentStorageFullStackTest]
 * for Google Drive setup and authorization flow tests.
 */
class SaDocumentsUploadGoogleDriveFullStackTest : SaFullStackTestBase() {

    @TempDir
    private lateinit var tempDir: Path

    @Test
    fun `should upload single file to Google Drive`(page: Page) {
        val fileContent = "Spaceship fuel receipt from Mars".toByteArray()
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
                val expense = expense(workspace = workspace)
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
        GoogleDriveApiMocks.mockFindFolder(
            parentFolderId = "root-folder-id",
            folderName = preconditions.workspace.id.toString(),
            responseFolderId = "workspace-folder-id",
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockUploadFile(
            responseId = "gdrive-uploaded-file-id",
            responseSize = fileContent.size.toLong(),
            expectedAuthToken = accessToken,
        )

        val testFile = createTestFile("fuel-receipt.pdf", fileContent)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveDocuments(DocumentsUpload.EmptyDocument)
                selectFileForUpload(testFile)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )
            }
            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        savedExpense.shouldWithClue("Expense should have one attachment") {
            attachments.shouldHaveSize(1)
        }

        val documentId = savedExpense.attachments.first().documentId
        val savedDocument = aggregateTemplate.findSingle<Document>(documentId)
        savedDocument.shouldWithClue("Document should be stored in Google Drive") {
            name.shouldBe(testFile.name)
            sizeInBytes.shouldBe(fileContent.size.toLong())
            storageId.shouldBe("google-drive")
            storageLocation.shouldBe("gdrive-uploaded-file-id")
        }

        GoogleDriveApiMocks.verifyUploadFileRequest()
    }

    @Test
    fun `should upload multiple files to Google Drive`(page: Page) {
        val file1Content = "Planet Express delivery log 3025".toByteArray()
        val file2Content = "Dark matter fuel invoice".toByteArray()

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
                val expense = expense(workspace = workspace)
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
        GoogleDriveApiMocks.mockFindFolder(
            parentFolderId = "root-folder-id",
            folderName = preconditions.workspace.id.toString(),
            responseFolderId = "workspace-folder-id",
            expectedAuthToken = accessToken,
        )

        val testFile1 = createNamedTestFile("delivery-log.pdf", file1Content)
        val testFile2 = createNamedTestFile("fuel-invoice.jpg", file2Content)

        GoogleDriveApiMocks.mockUploadFileForFileName(
            fileName = "delivery-log_${MOCK_EPOCH_MILLIS}.pdf",
            responseId = "gdrive-file-id-1",
            responseSize = file1Content.size.toLong(),
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockUploadFileForFileName(
            fileName = "fuel-invoice_${MOCK_EPOCH_MILLIS}.jpg",
            responseId = "gdrive-file-id-2",
            responseSize = file2Content.size.toLong(),
            expectedAuthToken = accessToken,
        )

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                selectFileForUpload(testFile1)
                selectFileForUpload(testFile2)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile1.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.UploadedDocument(testFile2.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )
            }
            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        savedExpense.shouldWithClue("Expense should have two attachments") {
            attachments.shouldHaveSize(2)
        }

        val documents = savedExpense.attachments.map { attachment ->
            aggregateTemplate.findSingle<Document>(attachment.documentId)
        }

        val storageLocations = documents.map { it.storageLocation }.toSet()
        storageLocations.shouldBe(setOf("gdrive-file-id-1", "gdrive-file-id-2"))

        documents.forEach { doc ->
            doc.shouldWithClue("Document '${doc.name}' should be stored in Google Drive") {
                storageId.shouldBe("google-drive")
            }
        }

        val doc1 = documents.find { it.storageLocation == "gdrive-file-id-1" }!!
        doc1.shouldWithClue("First uploaded document should have correct size") {
            sizeInBytes.shouldBe(file1Content.size.toLong())
        }

        val doc2 = documents.find { it.storageLocation == "gdrive-file-id-2" }!!
        doc2.shouldWithClue("Second uploaded document should have correct size") {
            sizeInBytes.shouldBe(file2Content.size.toLong())
        }
    }

    @Test
    fun `should download pre-existing document from Google Drive`(page: Page) {
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
                val expense = expense(
                    workspace = workspace,
                    attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "delivery-contract.pdf",
                            storageId = "google-drive",
                            storageLocation = "gdrive-existing-file-id",
                            sizeInBytes = documentContent.size.toLong(),
                            timeUploaded = MOCK_TIME
                        )
                    )
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
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("delivery-contract.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.EmptyDocument
                )

                val downloadedContent = downloadDocument("delivery-contract.pdf")
                downloadedContent.shouldBe(documentContent)
            }
        }
    }

    @Test
    fun `should create workspace folder in Google Drive on first upload`(page: Page) {
        val fileContent = "Slurm supplies order form".toByteArray()
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
                val expense = expense(workspace = workspace)
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
        GoogleDriveApiMocks.mockFindFolder(
            parentFolderId = "root-folder-id",
            folderName = preconditions.workspace.id.toString(),
            responseFolderId = null,
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockCreateFolder(
            requestName = preconditions.workspace.id.toString(),
            requestParents = listOf("root-folder-id"),
            responseId = "new-workspace-folder-id",
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockUploadFile(
            responseId = "gdrive-uploaded-file-id",
            responseSize = fileContent.size.toLong(),
            expectedAuthToken = accessToken,
        )

        val testFile = createTestFile("slurm-order.pdf", fileContent)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveDocuments(DocumentsUpload.EmptyDocument)
                selectFileForUpload(testFile)
            }
            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        savedExpense.shouldWithClue("Expense should have one attachment") {
            attachments.shouldHaveSize(1)
        }

        val documentId = savedExpense.attachments.first().documentId
        val savedDocument = aggregateTemplate.findSingle<Document>(documentId)
        savedDocument.shouldWithClue("Document should be stored in Google Drive") {
            name.shouldBe(testFile.name)
            storageId.shouldBe("google-drive")
            storageLocation.shouldBe("gdrive-uploaded-file-id")
        }

        GoogleDriveApiMocks.verifyCreateFolderRequest(
            folderName = preconditions.workspace.id.toString(),
            parentFolderId = "root-folder-id",
        )
    }

    @Test
    fun `should append epoch millis to storage filename while preserving original name in document`(page: Page) {
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
                val expense = expense(workspace = workspace)
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
        GoogleDriveApiMocks.mockFindFolder(
            parentFolderId = "root-folder-id",
            folderName = preconditions.workspace.id.toString(),
            responseFolderId = "workspace-folder-id",
            expectedAuthToken = accessToken,
        )

        // File with extension: slurm-can.pdf → storage name: slurm-can_<epochMillis>.pdf
        val fileWithExtContent = "Slurm delivery receipt".toByteArray()
        val fileWithExt = createNamedTestFile("slurm-can.pdf", fileWithExtContent)
        // File without extension: dark-matter-invoice → storage name: dark-matter-invoice_<epochMillis>
        val fileWithoutExtContent = "Dark matter fuel costs".toByteArray()
        val fileWithoutExt = createNamedTestFile("dark-matter-invoice", fileWithoutExtContent)
        // File with leading dot only (no real extension): .bender-config → storage name: .bender-config_<epochMillis>
        val fileWithLeadingDotContent = "Bender robot config".toByteArray()
        val fileWithLeadingDot = createNamedTestFile(".bender-config", fileWithLeadingDotContent)

        GoogleDriveApiMocks.mockUploadFileForFileName(
            fileName = "slurm-can_${MOCK_EPOCH_MILLIS}.pdf",
            responseId = "gdrive-id-with-ext",
            responseSize = fileWithExtContent.size.toLong(),
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockUploadFileForFileName(
            fileName = "dark-matter-invoice_${MOCK_EPOCH_MILLIS}",
            responseId = "gdrive-id-without-ext",
            responseSize = fileWithoutExtContent.size.toLong(),
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockUploadFileForFileName(
            fileName = ".bender-config_${MOCK_EPOCH_MILLIS}",
            responseId = "gdrive-id-leading-dot",
            responseSize = fileWithLeadingDotContent.size.toLong(),
            expectedAuthToken = accessToken,
        )

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                selectFileForUpload(fileWithExt)
                selectFileForUpload(fileWithoutExt)
                selectFileForUpload(fileWithLeadingDot)
            }
            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        savedExpense.shouldWithClue("Expense should have three attachments") {
            attachments.shouldHaveSize(3)
        }

        val documentsByLocation = savedExpense.attachments
            .map { aggregateTemplate.findSingle<Document>(it.documentId) }
            .associateBy { it.storageLocation }

        documentsByLocation["gdrive-id-with-ext"]!!.shouldWithClue("File with extension: original name preserved") {
            name.shouldBe("slurm-can.pdf")
            storageId.shouldBe("google-drive")
        }
        documentsByLocation["gdrive-id-without-ext"]!!.shouldWithClue("File without extension: original name preserved") {
            name.shouldBe("dark-matter-invoice")
            storageId.shouldBe("google-drive")
        }
        documentsByLocation["gdrive-id-leading-dot"]!!.shouldWithClue("File with leading dot: original name preserved") {
            name.shouldBe(".bender-config")
            storageId.shouldBe("google-drive")
        }
    }

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = Files.createTempFile(tempDir, "test-upload-gdrive-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }

    private fun createNamedTestFile(fileName: String, content: ByteArray): Path {
        return tempDir.resolve(fileName).also { it.writeBytes(content) }
    }
}
