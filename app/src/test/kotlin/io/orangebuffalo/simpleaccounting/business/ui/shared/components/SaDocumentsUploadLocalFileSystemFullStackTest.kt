package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

/**
 * Full stack tests for DocumentsUpload component (SaDocumentsUpload) with LocalFileSystemDocumentsStorage integration.
 * Unlike [SaDocumentsUploadGoogleDriveFullStackTest] that uses Google Drive, this test verifies the real local file
 * system storage integration where uploaded files are stored in a temporary directory.
 * Uses Edit Expense page as testing grounds.
 */
class SaDocumentsUploadLocalFileSystemFullStackTest : SaFullStackTestBase() {

    @TempDir
    private lateinit var tempDir: Path

    @TempDir
    private lateinit var uploadTempDir: Path

    @BeforeEach
    fun setupLocalFsStorage() {
        whenever(localFsStorageProperties.baseDirectory) doReturn tempDir
    }

    @Test
    fun `should upload single file to local file system`(page: Page) {
        val fileContent = "Spaceship fuel receipt from Mars".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "local-fs")
                val workspace = workspace(owner = fry)
                val expense = expense(workspace = workspace)
            }
        }

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
        savedDocument.shouldWithClue("Document should be stored in local file system") {
            name.shouldBe(testFile.name)
            sizeInBytes.shouldBe(fileContent.size.toLong())
            storageId.shouldBe("local-fs")
            mimeType.shouldBe("application/pdf")
        }

        val storedFile = tempDir.resolve(savedDocument.storageLocation!!)
        storedFile.readBytes().shouldBe(fileContent)
    }

    @Test
    fun `should upload multiple files to local file system`(page: Page) {
        val file1Content = "Planet Express delivery log 3025".toByteArray()
        val file2Content = "Dark matter fuel invoice".toByteArray()

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "local-fs")
                val workspace = workspace(owner = fry)
                val expense = expense(workspace = workspace)
            }
        }

        val testFile1 = createTestFile("delivery-log.pdf", file1Content)
        val testFile2 = createTestFile("fuel-invoice.jpg", file2Content)

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

        documents.forEach { doc ->
            doc.shouldWithClue("Document '${doc.name}' should be stored in local file system") {
                storageId.shouldBe("local-fs")
            }
        }

        val doc1 = documents.find { it.name == testFile1.name }!!
        doc1.shouldWithClue("First uploaded document should have correct content and content type") {
            tempDir.resolve(storageLocation!!).readBytes().shouldBe(file1Content)
            mimeType.shouldBe("application/pdf")
        }

        val doc2 = documents.find { it.name == testFile2.name }!!
        doc2.shouldWithClue("Second uploaded document should have correct content and content type") {
            tempDir.resolve(storageLocation!!).readBytes().shouldBe(file2Content)
            mimeType.shouldBe("image/jpeg")
        }
    }

    @Test
    fun `should download pre-existing document from local file system`(page: Page) {
        val documentContent = "Good news, everyone! Delivery contract for Omicron Persei 8".toByteArray()

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "local-fs")
                val workspace = workspace(owner = fry)
                val expense = expense(
                    workspace = workspace,
                    attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "delivery-contract.pdf",
                            storageId = "local-fs",
                            storageLocation = "${workspace.id}/existing-file.pdf",
                            sizeInBytes = documentContent.size.toLong(),
                            timeUploaded = MOCK_TIME,
                            mimeType = "application/pdf"
                        )
                    )
                )
            }
        }

        val storedFile = tempDir.resolve("${preconditions.workspace.id}/existing-file.pdf")
        Files.createDirectories(storedFile.parent)
        storedFile.writeBytes(documentContent)

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
    fun `should store uploaded document in year-month subfolder`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "local-fs")
                val workspace = workspace(owner = fry)
                val expense = expense(workspace = workspace)
            }
        }

        val testFile = createTestFile("robot-oil-receipt.pdf", "Robot oil receipt".toByteArray())

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                selectFileForUpload(testFile)
            }
            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        val documentId = savedExpense.attachments.first().documentId
        val savedDocument = aggregateTemplate.findSingle<Document>(documentId)
        savedDocument.storageLocation!!.shouldStartWith("${preconditions.workspace.id}/1999-03/")
        savedDocument.mimeType.shouldBe("application/pdf")
        val storedFile = tempDir.resolve(savedDocument.storageLocation!!)
        storedFile.readBytes().shouldBe("Robot oil receipt".toByteArray())
    }

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = Files.createTempFile(uploadTempDir, "test-upload-local-fs-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }
}
