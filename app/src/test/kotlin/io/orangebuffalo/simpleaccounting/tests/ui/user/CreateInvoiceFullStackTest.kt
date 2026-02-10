package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateInvoicePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateInvoicePage.Companion.openCreateInvoicePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.InvoicesOverviewPage.Companion.shouldBeInvoicesOverviewPage
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.io.path.name
import kotlin.io.path.writeBytes

class CreateInvoiceFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should create invoice with basic fields`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            customer { input.selectOption("Spaceship Repairs Inc") }
            title { input.fill("Delivery services") }
            amount { input.fill("150.00") }
            dateIssued { input.fill("3025-01-15") }
            dueDate { input.fill("3025-02-15") }

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>()
            .shouldBeEntityWithFields(
                Invoice(
                    title = "Delivery services",
                    customerId = preconditions.customer.id!!,
                    dateIssued = LocalDate.of(3025, 1, 15),
                    dueDate = LocalDate.of(3025, 2, 15),
                    currency = "USD",
                    amount = 15000,
                    status = InvoiceStatus.DRAFT,
                    timeRecorded = MOCK_TIME,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Invoice::id,
                    Invoice::version,
                    Invoice::attachments,
                )
            )
    }

    @Test
    fun `should create invoice with general tax`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            customer { input.selectOption("Spaceship Repairs Inc") }
            title { input.fill("Taxable delivery") }
            amount { input.fill("100.00") }
            dateIssued { input.fill("3025-01-15") }
            dueDate { input.fill("3025-02-15") }
            generalTax { input.selectOption("VAT") }

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>()
            .should {
                it.generalTaxId.shouldBe(preconditions.generalTax.id)
            }
    }

    @Test
    fun `should create invoice with notes`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            customer { input.selectOption("Spaceship Repairs Inc") }
            title { input.fill("Delivery to Omicron Persei 8") }
            amount { input.fill("75.00") }
            dateIssued { input.fill("3025-01-15") }
            dueDate { input.fill("3025-02-15") }
            notes {
                input.fill("# Payment Terms\n\nNet 30 days")
                input.shouldHavePreviewWithHeading("Payment Terms")
            }

            reportRendering("create-invoice.with-notes")

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>()
            .should {
                it.notes.shouldBe("# Payment Terms\n\nNet 30 days")
            }
    }

    @Test
    fun `should create invoice with document attachments`(page: Page) {
        val file1Content = "Service agreement document".toByteArray()
        val file2Content = "Work completion certificate".toByteArray()

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry).also {
                    customer(workspace = it, name = "Spaceship Repairs Inc")
                }
            }
        }

        val testFile1 = createTestFile("agreement.pdf", file1Content)
        val testFile2 = createTestFile("certificate.pdf", file2Content)

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateInvoicePage {
            customer { input.selectOption("Spaceship Repairs Inc") }
            title { input.fill("Moon cargo delivery") }
            amount { input.fill("200.00") }
            dateIssued { input.fill("3025-01-15") }
            dueDate { input.fill("3025-02-15") }

            documentsUpload {
                // Upload first document
                selectFileForUpload(testFile1)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile1.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )

                // Upload second document
                selectFileForUpload(testFile2)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile1.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.UploadedDocument(testFile2.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )
            }

            reportRendering("create-invoice.with-attachments")

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        // Verify invoice was created with two attachments
        val savedInvoice = aggregateTemplate.findSingle<Invoice>()
        savedInvoice.shouldWithClue("Invoice should have two attachments") {
            attachments.shouldHaveSize(2)
        }

        // Verify both documents were saved with correct metadata and content
        val documents = savedInvoice.attachments.map { attachment ->
            aggregateTemplate.findSingle<Document>(attachment.documentId)
        }

        val doc1 = documents.find { it.name == testFile1.name }!!
        doc1.shouldWithClue("First document metadata should be correct") {
            this.name.shouldBe(testFile1.name)
            this.sizeInBytes.shouldBe(file1Content.size.toLong())
            this.storageId.shouldBe(TestDocumentsStorage.STORAGE_ID)
            this.workspaceId.shouldBe(preconditions.workspace.id)
        }
        testDocumentsStorage.getUploadedContent(doc1.storageLocation!!).shouldBe(file1Content)

        val doc2 = documents.find { it.name == testFile2.name }!!
        doc2.shouldWithClue("Second document metadata should be correct") {
            this.name.shouldBe(testFile2.name)
            this.sizeInBytes.shouldBe(file2Content.size.toLong())
            this.storageId.shouldBe(TestDocumentsStorage.STORAGE_ID)
            this.workspaceId.shouldBe(preconditions.workspace.id)
        }
        testDocumentsStorage.getUploadedContent(doc2.storageLocation!!).shouldBe(file2Content)
    }

    private fun createTestFile(fileName: String, content: ByteArray): java.nio.file.Path {
        val testFile = java.nio.file.Files.createTempFile("test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }

    @Test
    fun `should create invoice with sent and paid dates`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            customer { input.selectOption("Spaceship Repairs Inc") }
            title { input.fill("Robot repair service") }
            amount { input.fill("300.00") }
            dateIssued { input.fill("3025-01-10") }
            dueDate { input.fill("3025-02-10") }

            // Mark as already sent
            alreadySent().click()
            dateSent().input.fill("3025-01-11")

            // Mark as already paid
            alreadyPaid().click()
            datePaid().input.fill("3025-01-20")

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>()
            .should {
                it.dateSent.shouldBe(LocalDate.of(3025, 1, 11))
                it.datePaid.shouldBe(LocalDate.of(3025, 1, 20))
                it.status.shouldBe(InvoiceStatus.PAID)
            }
    }

    @Test
    fun `should validate required fields`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            saveButton.click()

            // Verify all required fields show validation errors
            customer {
                shouldHaveValidationError("Please select a customer")
            }
            title {
                shouldHaveValidationError("Please provide the title")
            }
            amount {
                shouldHaveValidationError("Please provide invoice amount")
            }
            // Date Issued gets a default value from the clock, so it won't have a validation error
            dueDate {
                shouldHaveValidationError("Please provide the date when invoice is due")
            }

            reportRendering("create-invoice.validation-errors")

            // Currency should not have error as it has default value
            currency {
                shouldNotHaveValidationErrors()
            }
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            customer { input.selectOption("Spaceship Repairs Inc") }
            title { input.fill("Cancelled Mars trip") }

            cancelButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findAll<Invoice>()
            .shouldWithClue("No invoices should be created") {
                shouldHaveSize(0)
            }
    }

    @Test
    fun `should handle all UI states correctly`(page: Page) {
        data class ExpectedFieldsVisibility(
            val customer: Boolean = true,
            val title: Boolean = true,
            val currency: Boolean = true,
            val amount: Boolean = true,
            val dateIssued: Boolean = true,
            val dueDate: Boolean = true,
            val generalTax: Boolean = true,
            val dateSent: Boolean = false,
            val datePaid: Boolean = false,
            val notes: Boolean = true,
            val saveButton: Boolean = true,
            val cancelButton: Boolean = true
        )

        fun CreateInvoicePage.verifyFieldsVisibility(expected: ExpectedFieldsVisibility) {
            // Always visible basic fields
            if (expected.customer) customer.shouldBeVisible() else customer.shouldBeHidden()
            if (expected.title) title.shouldBeVisible() else title.shouldBeHidden()
            if (expected.currency) currency.shouldBeVisible() else currency.shouldBeHidden()
            if (expected.amount) amount.shouldBeVisible() else amount.shouldBeHidden()
            if (expected.dateIssued) dateIssued.shouldBeVisible() else dateIssued.shouldBeHidden()
            if (expected.dueDate) dueDate.shouldBeVisible() else dueDate.shouldBeHidden()
            if (expected.generalTax) generalTax.shouldBeVisible() else generalTax.shouldBeHidden()
            if (expected.notes) notes.shouldBeVisible() else notes.shouldBeHidden()
            if (expected.saveButton) saveButton.shouldBeVisible() else saveButton.shouldBeHidden()
            if (expected.cancelButton) cancelButton.shouldBeVisible() else cancelButton.shouldBeHidden()

            // Conditionally visible - sent/paid date fields
            if (expected.dateSent) {
                dateSent().shouldBeVisible()
            } else {
                dateSent().shouldBeHidden()
            }

            if (expected.datePaid) {
                datePaid().shouldBeVisible()
            } else {
                datePaid().shouldBeHidden()
            }
        }

        page.setupPreconditionsAndNavigateToCreatePage {
            // Initial state - all basic fields visible, conditional fields hidden
            var expectedState = ExpectedFieldsVisibility()
            verifyFieldsVisibility(expectedState)
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
            }

            reportRendering("create-invoice.initial-state")

            // Enable already sent - date sent field appears
            alreadySent().click()
            expectedState = expectedState.copy(dateSent = true)
            verifyFieldsVisibility(expectedState)

            reportRendering("create-invoice.already-sent-enabled")

            // Enable already paid - date paid field appears
            alreadyPaid().click()
            expectedState = expectedState.copy(datePaid = true)
            verifyFieldsVisibility(expectedState)

            reportRendering("create-invoice.already-paid-enabled")

            // Disable already sent - date sent field disappears
            alreadySent().click()
            expectedState = expectedState.copy(dateSent = false)
            verifyFieldsVisibility(expectedState)

            reportRendering("create-invoice.already-sent-disabled")

            // Disable already paid - date paid field disappears
            alreadyPaid().click()
            expectedState = expectedState.copy(datePaid = false)
            verifyFieldsVisibility(expectedState)

            reportRendering("create-invoice.already-paid-disabled")
        }
    }

    @Test
    fun `should display dropdown options correctly`(page: Page) {
        val testPreconditions = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    customer(workspace = workspace, name = "Nibbler Enterprises")
                    customer(workspace = workspace, name = "Customer A")
                    customer(workspace = workspace, name = "Hypnotoad Productions")
                    generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
                    generalTax(workspace = workspace, title = "GST", rateInBps = 1000)
                }
            }
        }

        page.authenticateViaCookie(testPreconditions.fry)
        page.openCreateInvoicePage {
            // Verify customer dropdown - customers API sorts by ID descending
            // Created in order: C (ID 1), A (ID 2), B (ID 3)
            // Expected order: B (ID 3), A (ID 2), C (ID 1)
            customer {
                input.shouldHaveOptions("Customer B", "Customer A", "Customer C")
            }

            // Verify general tax dropdown - taxes are sorted alphabetically by title
            generalTax {
                input.shouldHaveOptions("GST", "VAT")
            }
        }
    }

    private fun Page.setupPreconditionsAndNavigateToCreatePage(spec: CreateInvoicePage.() -> Unit) {
        authenticateViaCookie(preconditions.fry)
        openCreateInvoicePage(spec)
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val customer = customer(workspace = workspace, name = "Spaceship Repairs Inc")
            val generalTax = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
        }
    }
}
