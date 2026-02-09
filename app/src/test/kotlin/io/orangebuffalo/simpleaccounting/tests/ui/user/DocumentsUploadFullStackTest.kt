package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.writeBytes

/**
 * Comprehensive full stack tests for DocumentsUpload component (SaDocumentsUpload).
 * Uses Edit Expense page as testing grounds.
 */
class DocumentsUploadFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display error when storage is not configured`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = null)
                val expense = expense(workspace = workspace(owner = fry))
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveStorageErrorMessage("Documents storage is not active")
                this@shouldBeEditExpensePage.reportRendering("documents-upload.storage-not-configured")
            }
        }
    }

    @Test
    fun `should display error when storage is not active`(page: Page) {
        testDocumentsStorage.setStorageStatus(active = false)

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val expense = expense(workspace = workspace(owner = fry))
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveStorageErrorMessage("Documents storage is not active")
                this@shouldBeEditExpensePage.reportRendering("documents-upload.storage-not-ready")
            }
        }
    }

    @Test
    fun `should show empty upload slot when no documents exist`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val expense = expense(workspace = workspace(owner = fry))
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveDocuments(DocumentsUpload.EmptyDocument)
                this@shouldBeEditExpensePage.reportRendering("documents-upload.empty-upload-slot")
            }
        }
    }

    @Test
    fun `should load and display single pre-existing document`(page: Page) {
        val documentContent = "Pre-existing document content".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val doc = document(
                    workspace = workspace,
                    name = "existing-receipt.pdf",
                    storageId = TestDocumentsStorage.STORAGE_ID,
                    storageLocation = "existing-location",
                    sizeInBytes = documentContent.size.toLong(),
                    timeUploaded = MOCK_TIME
                )
                val expense = expense(workspace = workspace, attachments = setOf(doc))
            }
        }

        testDocumentsStorage.mockDocumentContent("existing-location", documentContent)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("existing-receipt.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.EmptyDocument
                )
                this@shouldBeEditExpensePage.reportRendering("documents-upload.single-pre-existing")

                // Verify download link works and content matches
                val downloadedContent = downloadDocument("existing-receipt.pdf")
                downloadedContent.shouldBe(documentContent)
            }
        }
    }

    @Test
    fun `should load and display multiple pre-existing documents`(page: Page) {
        val document1Content = "First document content".toByteArray()
        val document2Content = "Second document content".toByteArray()
        val document3Content = "Third document content".toByteArray()

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val expense = expense(
                    workspace = workspace, attachments = setOf(
                        document(
                            workspace = workspace,
                            name = "contract.docx",
                            storageId = TestDocumentsStorage.STORAGE_ID,
                            storageLocation = "location-3",
                            sizeInBytes = document3Content.size.toLong(),
                            timeUploaded = MOCK_TIME
                        ), document(
                            workspace = workspace,
                            name = "invoice.pdf",
                            storageId = TestDocumentsStorage.STORAGE_ID,
                            storageLocation = "location-1",
                            sizeInBytes = document1Content.size.toLong(),
                            timeUploaded = MOCK_TIME
                        ), document(
                            workspace = workspace,
                            name = "receipt.jpg",
                            storageId = TestDocumentsStorage.STORAGE_ID,
                            storageLocation = "location-2",
                            sizeInBytes = document2Content.size.toLong(),
                            timeUploaded = MOCK_TIME
                        )
                    )
                )
            }
        }

        testDocumentsStorage.mockDocumentContent("location-1", document1Content)
        testDocumentsStorage.mockDocumentContent("location-2", document2Content)
        testDocumentsStorage.mockDocumentContent("location-3", document3Content)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("contract.docx", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument("invoice.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument("receipt.jpg", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.EmptyDocument
                )
                this@shouldBeEditExpensePage.reportRendering("documents-upload.multiple-pre-existing")
            }
        }
    }

    @Test
    fun `should remove pre-existing document before save`(page: Page) {
        val documentContent = "Document to be removed".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val doc = document(
                    workspace = workspace,
                    name = "to-remove.pdf",
                    storageId = TestDocumentsStorage.STORAGE_ID,
                    storageLocation = "remove-location",
                    sizeInBytes = documentContent.size.toLong(),
                    timeUploaded = MOCK_TIME
                )
                val expense = expense(workspace = workspace, attachments = setOf(doc))
            }
        }

        testDocumentsStorage.mockDocumentContent("remove-location", documentContent)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("to-remove.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.EmptyDocument
                )
                removeDocument("to-remove.pdf")
                shouldHaveDocuments(DocumentsUpload.EmptyDocument)
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        // Verify document was removed
        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        savedExpense.shouldWithClue("Expense should have no attachments after removal") {
            attachments.shouldBe(emptySet())
        }
    }

    @Test
    fun `should upload single file and verify content`(page: Page) {
        val fileContent = "Single file upload test content".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val expense = expense(workspace = workspace)
            }
        }

        val testFile = createTestFile("upload-test.pdf", fileContent)

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
                this@shouldBeEditExpensePage.reportRendering("documents-upload.file-selected-pending")
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        // Verify database state
        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        savedExpense.shouldWithClue("Expense should have one attachment") {
            attachments.shouldHaveSize(1)
        }

        val documentId = savedExpense.attachments.first().documentId
        val savedDocument = aggregateTemplate.findSingle<Document>(documentId)
        savedDocument.shouldWithClue("Document should have correct metadata") {
            name.shouldBe(testFile.name)
            sizeInBytes.shouldBe(fileContent.size.toLong())
            storageId.shouldBe(TestDocumentsStorage.STORAGE_ID)
        }

        // Verify uploaded content matches original (binary equality)
        val uploadedContent = testDocumentsStorage.getUploadedContent(savedDocument.storageLocation!!)
        uploadedContent.shouldBe(fileContent)
    }

    @Test
    fun `should upload multiple files and verify content`(page: Page) {
        val file1Content = "First upload test content".toByteArray()
        val file2Content = "Second upload test content".toByteArray()
        val file3Content = "Third upload test content".toByteArray()

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val expense = expense(workspace = workspace)
            }
        }

        val testFile1 = createTestFile("upload1.pdf", file1Content)
        val testFile2 = createTestFile("upload2.jpg", file2Content)
        val testFile3 = createTestFile("upload3.docx", file3Content)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                // Select first file
                selectFileForUpload(testFile1)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile1.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )

                // Select second file
                selectFileForUpload(testFile2)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile1.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.UploadedDocument(testFile2.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )

                // Select third file
                selectFileForUpload(testFile3)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile1.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.UploadedDocument(testFile2.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.UploadedDocument(testFile3.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )
                this@shouldBeEditExpensePage.reportRendering("documents-upload.multiple-files-pending")
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        // Verify all three files were uploaded
        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        savedExpense.shouldWithClue("Expense should have three attachments") {
            attachments.shouldHaveSize(3)
        }

        // Verify content of each uploaded file (binary equality)
        val documents = savedExpense.attachments.map { attachment ->
            aggregateTemplate.findSingle<Document>(attachment.documentId)
        }

        val doc1 = documents.find { it.name == testFile1.name }!!
        testDocumentsStorage.getUploadedContent(doc1.storageLocation!!).shouldBe(file1Content)

        val doc2 = documents.find { it.name == testFile2.name }!!
        testDocumentsStorage.getUploadedContent(doc2.storageLocation!!).shouldBe(file2Content)

        val doc3 = documents.find { it.name == testFile3.name }!!
        testDocumentsStorage.getUploadedContent(doc3.storageLocation!!).shouldBe(file3Content)
    }

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = Files.createTempFile("test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }
}
