package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.test.context.ContextConfiguration
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes

/**
 * Comprehensive full stack tests for DocumentsUpload component (SaDocumentsUpload).
 * Uses Edit Expense page as testing grounds.
 */
@ContextConfiguration(classes = [DocumentsUploadFullStackTest.TestDocumentsStorageConfig::class])
class DocumentsUploadFullStackTest : SaFullStackTestBase() {

    @Autowired
    private lateinit var testDocumentsStorage: TestDocumentsStorage

    @BeforeEach
    fun setup(page: Page) {
        mockCurrentTime(timeService)
        // Resume clock to allow debouncing and async operations to work
        page.clock().resume()
    }

    @TestConfiguration
    class TestDocumentsStorageConfig {
        @Bean
        fun testDocumentsStorage() = TestDocumentsStorage()
    }

    class TestDocumentsStorage(
        val mockStorage: DocumentsStorage = org.mockito.kotlin.mock()
    ) : DocumentsStorage by mockStorage {
        private val uploadedDocuments = mutableMapOf<String, ByteArray>()

        init {
            // Always return "test-storage" as ID
            whenever(mockStorage.getId()) doReturn "test-storage"
        }

        fun mockStorageActive() {
            mockStorage.stub {
                onBlocking { getCurrentUserStorageStatus() } doReturn DocumentsStorageStatus(active = true)
            }
        }

        fun mockStorageInactive() {
            mockStorage.stub {
                onBlocking { getCurrentUserStorageStatus() } doReturn DocumentsStorageStatus(active = false)
            }
        }

        fun mockDocumentUpload(fileName: String, content: ByteArray): String {
            val storageLocation = "storage-location-${uploadedDocuments.size + 1}"
            uploadedDocuments[storageLocation] = content

            mockStorage.stub {
                onBlocking {
                    saveDocument(argThat { this.fileName == fileName })
                } doReturn SaveDocumentResponse(
                    storageLocation = storageLocation,
                    sizeInBytes = content.size.toLong()
                )
            }
            return storageLocation
        }

        fun mockDocumentContent(storageLocation: String, content: ByteArray) {
            mockStorage.stub {
                onBlocking {
                    getDocumentContent(any(), argThat { this == storageLocation })
                } doReturn flowOf(
                    DefaultDataBufferFactory.sharedInstance.wrap(content)
                )
            }
        }

        fun getUploadedContent(storageLocation: String): ByteArray {
            return uploadedDocuments[storageLocation]
                ?: throw IllegalStateException("No content found for location: $storageLocation")
        }
    }

    @Test
    fun `should display loading state when storage status is being checked`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 0,
                    currency = "USD",
                    attachments = emptySet()
                )
            }
        }

        // Don't mock storage status - it will be in loading state initially
        testDocumentsStorage.mockStorage.stub {
            onBlocking { getCurrentUserStorageStatus() } doReturn DocumentsStorageStatus(active = false)
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveLoadingPlaceholder()
                reportRendering("documents-upload.initial-loading-state")
            }
        }
    }

    @Test
    fun `should display error when storage is not configured`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = null)
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 0,
                    currency = "USD",
                    attachments = emptySet()
                )
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
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 0,
                    currency = "USD",
                    attachments = emptySet()
                )
            }
        }

        testDocumentsStorage.mockStorageInactive()

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
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 0,
                    currency = "USD",
                    attachments = emptySet()
                )
            }
        }

        testDocumentsStorage.mockStorageActive()

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveEmptyUploadSlot()
                reportRendering("documents-upload.empty-upload-slot")
            }
        }
    }

    @Test
    fun `should upload single file and verify content`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 0,
                    currency = "USD",
                    attachments = emptySet()
                )
            }
        }

        testDocumentsStorage.mockStorageActive()

        val testFileContent = "Test file content for upload".toByteArray()
        val testFile = createTestFile("test-receipt.pdf", testFileContent)
        val storageLocation = testDocumentsStorage.mockDocumentUpload("test-receipt.pdf", testFileContent)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                // Wait for component to be ready (empty slot should be visible)
                shouldHaveEmptyUploadSlot()
                
                uploadFile(testFile)
                reportRendering("documents-upload.single-file-selected")

                shouldHaveDocuments(1)
                shouldHaveDocumentWithName("test-receipt.pdf")
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        // Verify database state
        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        savedExpense.shouldWithClue("Expense should have one attachment") {
            attachments.shouldHaveSize(1)
        }

        val attachment = savedExpense.attachments.first()
        val savedDocument = aggregateTemplate.findSingle<Document>(attachment.documentId)
        savedDocument.shouldWithClue("Document should have correct metadata") {
            name.shouldBe("test-receipt.pdf")
            sizeInBytes.shouldBe(testFileContent.size.toLong())
            storageId.shouldBe("test-storage")
            this.storageLocation.shouldBe(storageLocation)
        }

        // Verify uploaded content matches original
        val uploadedContent = testDocumentsStorage.getUploadedContent(storageLocation)
        uploadedContent.shouldBe(testFileContent)
    }

    @Test
    fun `should load and display single pre-existing document`(page: Page) {
        val documentContent = "Pre-existing document content".toByteArray()

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val document = document(
                    workspace = workspace,
                    name = "existing-receipt.pdf",
                    storageId = "test-storage",
                    storageLocation = "existing-location",
                    sizeInBytes = documentContent.size.toLong(),
                    timeUploaded = MOCK_TIME
                )
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 0,
                    currency = "USD",
                    attachments = setOf(document)
                )
            }
        }

        testDocumentsStorage.mockStorageActive()
        testDocumentsStorage.mockDocumentContent("existing-location", documentContent)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveDocuments(2) // 1 existing + 1 empty slot
                shouldHaveDocumentWithName("existing-receipt.pdf")
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
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
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
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 0,
                    currency = "USD",
                    attachments = setOf(doc1, doc2, doc3)
                )
            }
        }

        testDocumentsStorage.mockStorageActive()
        testDocumentsStorage.mockDocumentContent("location-1", document1Content)
        testDocumentsStorage.mockDocumentContent("location-2", document2Content)
        testDocumentsStorage.mockDocumentContent("location-3", document3Content)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveDocuments(4) // 3 existing + 1 empty slot
                shouldHaveDocumentWithName("invoice.pdf")
                shouldHaveDocumentWithName("receipt.jpg")
                shouldHaveDocumentWithName("contract.docx")
                reportRendering("documents-upload.multiple-pre-existing")
            }
        }
    }

    @Test
    fun `should upload multiple files`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 0,
                    currency = "USD",
                    attachments = emptySet()
                )
            }
        }

        testDocumentsStorage.mockStorageActive()

        val file1Content = "First file content".toByteArray()
        val file2Content = "Second file content".toByteArray()
        val file3Content = "Third file content".toByteArray()

        val file1 = createTestFile("receipt1.pdf", file1Content)
        val file2 = createTestFile("receipt2.pdf", file2Content)
        val file3 = createTestFile("receipt3.pdf", file3Content)

        testDocumentsStorage.mockDocumentUpload("receipt1.pdf", file1Content)
        testDocumentsStorage.mockDocumentUpload("receipt2.pdf", file2Content)
        testDocumentsStorage.mockDocumentUpload("receipt3.pdf", file3Content)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                uploadFile(file1)
                uploadFile(file2)
                uploadFile(file3)

                shouldHaveDocuments(3)
                shouldHaveDocumentWithName("receipt1.pdf")
                shouldHaveDocumentWithName("receipt2.pdf")
                shouldHaveDocumentWithName("receipt3.pdf")
                reportRendering("documents-upload.multiple-files-selected")
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        // Verify all three documents were saved
        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        savedExpense.shouldWithClue("Expense should have three attachments") {
            attachments.shouldHaveSize(3)
        }
    }

    @Test
    fun `should remove pre-existing document before save`(page: Page) {
        val documentContent = "Document to be removed".toByteArray()

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val document = document(
                    workspace = workspace,
                    name = "to-remove.pdf",
                    storageId = "test-storage",
                    storageLocation = "remove-location",
                    sizeInBytes = documentContent.size.toLong(),
                    timeUploaded = MOCK_TIME
                )
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 0,
                    currency = "USD",
                    attachments = setOf(document)
                )
            }
        }

        testDocumentsStorage.mockStorageActive()
        testDocumentsStorage.mockDocumentContent("remove-location", documentContent)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                shouldHaveDocumentWithName("to-remove.pdf")
                reportRendering("documents-upload.before-removal")

                removeDocument("to-remove.pdf")
                shouldNotHaveDocument("to-remove.pdf")
                reportRendering("documents-upload.after-removal")
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        // Verify document was removed
        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        savedExpense.shouldWithClue("Expense should have no attachments after removal") {
            attachments.shouldHaveSize(0)
        }
    }

    @Test
    fun `should handle mixed scenario with existing and new documents`(page: Page) {
        val existingContent = "Existing document content".toByteArray()
        val newFileContent = "New file content".toByteArray()

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val existingDoc = document(
                    workspace = workspace,
                    name = "existing.pdf",
                    storageId = "test-storage",
                    storageLocation = "existing-loc",
                    sizeInBytes = existingContent.size.toLong(),
                    timeUploaded = MOCK_TIME
                )
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 0,
                    currency = "USD",
                    attachments = setOf(existingDoc)
                )
            }
        }

        testDocumentsStorage.mockStorageActive()
        testDocumentsStorage.mockDocumentContent("existing-loc", existingContent)

        val newFile = createTestFile("new-receipt.pdf", newFileContent)
        testDocumentsStorage.mockDocumentUpload("new-receipt.pdf", newFileContent)

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")

        page.shouldBeEditExpensePage {
            documentsUpload {
                // Verify existing document is present
                shouldHaveDocumentWithName("existing.pdf")

                // Add new document
                uploadFile(newFile)
                shouldHaveDocumentWithName("new-receipt.pdf")

                // Should now have both
                shouldHaveDocuments(2)
                reportRendering("documents-upload.mixed-existing-and-new")
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        // Verify both documents are saved
        val savedExpense = aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
        savedExpense.shouldWithClue("Expense should have two attachments") {
            attachments.shouldHaveSize(2)
        }
    }

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = Files.createTempFile("test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }
}
