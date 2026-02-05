package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes

/**
 * Comprehensive full stack tests for DocumentsUpload component (SaDocumentsUpload).
 * Uses Edit Expense page as testing grounds.
 */
class DocumentsUploadFullStackTest : SaFullStackTestBase() {

    @BeforeEach
    fun setup(page: Page) {
        mockCurrentTime(timeService)
        testDocumentsStorage.reset()
        // Resume clock to allow debouncing and async operations to work
        page.clock().resume()
    }

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
                shouldHaveStorageErrorMessage()
                reportRendering("documents-upload.storage-not-configured")
            }
        }
    }

    @Test
    fun `should display error when storage is not active`(page: Page) {
        testDocumentsStorage.setStorageStatus(active = false)

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val expense = expense(workspace = workspace(owner = fry))
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveStorageErrorMessage()
                reportRendering("documents-upload.storage-not-ready")
            }
        }
    }

    @Test
    fun `should show empty upload slot when no documents exist`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val expense = expense(workspace = workspace(owner = fry))
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("", DocumentsUpload.DocumentState.EMPTY)
                )
                reportRendering("documents-upload.empty-upload-slot")
            }
        }
    }

    @Test
    fun `should load and display single pre-existing document`(page: Page) {
        val documentContent = "Pre-existing document content".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry)
                val doc = document(
                    workspace = workspace,
                    name = "existing-receipt.pdf",
                    storageId = "test-storage",
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
                    DocumentsUpload.UploadedDocument("", DocumentsUpload.DocumentState.EMPTY)
                )
                reportRendering("documents-upload.single-pre-existing")

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
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry)
                val doc1 = document(
                    workspace = workspace,
                    name = "invoice.pdf",
                    storageId = "test-storage",
                    storageLocation = "location-1",
                    sizeInBytes = document1Content.size.toLong(),
                    timeUploaded = MOCK_TIME
                )
                val doc2 = document(
                    workspace = workspace,
                    name = "receipt.jpg",
                    storageId = "test-storage",
                    storageLocation = "location-2",
                    sizeInBytes = document2Content.size.toLong(),
                    timeUploaded = MOCK_TIME
                )
                val doc3 = document(
                    workspace = workspace,
                    name = "contract.docx",
                    storageId = "test-storage",
                    storageLocation = "location-3",
                    sizeInBytes = document3Content.size.toLong(),
                    timeUploaded = MOCK_TIME
                )
                val expense = expense(workspace = workspace, attachments = setOf(doc1, doc2, doc3))
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
                    DocumentsUpload.UploadedDocument("invoice.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument("receipt.jpg", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument("contract.docx", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument("", DocumentsUpload.DocumentState.EMPTY)
                )
                reportRendering("documents-upload.multiple-pre-existing")
            }
        }
    }

    @Test
    fun `should remove pre-existing document before save`(page: Page) {
        val documentContent = "Document to be removed".toByteArray()
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry)
                val doc = document(
                    workspace = workspace,
                    name = "to-remove.pdf",
                    storageId = "test-storage",
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
                    DocumentsUpload.UploadedDocument("", DocumentsUpload.DocumentState.EMPTY)
                )
                reportRendering("documents-upload.before-removal")

                removeDocument("to-remove.pdf")
                
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("", DocumentsUpload.DocumentState.EMPTY)
                )
                reportRendering("documents-upload.after-removal")
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

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = Files.createTempFile("test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }
}
