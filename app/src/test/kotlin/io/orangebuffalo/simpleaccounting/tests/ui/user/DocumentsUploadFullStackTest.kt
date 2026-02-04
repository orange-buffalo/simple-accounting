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

        fun mockStorageActive() {
            whenever(mockStorage.getId()) doReturn "test-storage"
            mockStorage.stub {
                onBlocking { getCurrentUserStorageStatus() } doReturn DocumentsStorageStatus(active = true)
            }
        }

        fun mockStorageInactive() {
            whenever(mockStorage.getId()) doReturn "test-storage"
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

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = Files.createTempFile("test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }
}
