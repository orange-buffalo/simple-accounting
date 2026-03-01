package io.orangebuffalo.simpleaccounting.business.ui.user.invoices

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.EditInvoicePage.Companion.shouldBeEditInvoicePage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.InvoicesOverviewPage.Companion.shouldBeInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.name
import kotlin.io.path.writeBytes

class EditInvoiceFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should load invoice with basic fields`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Spaceship Repairs Inc")
                val invoice = invoice(
                    customer = customer,
                    title = "Delivery services",
                    currency = "USD",
                    amount = 15000,
                    dateIssued = LocalDate.of(3025, 1, 15),
                    dueDate = LocalDate.of(3025, 2, 15),
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            customer {
                input.shouldHaveSelectedValue("Spaceship Repairs Inc")
            }
            title {
                input.shouldHaveValue("Delivery services")
            }
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
            }
            amount {
                input.shouldHaveValue("150.00")
            }
            dateIssued {
                input.shouldHaveValue("3025-01-15")
            }
            dueDate {
                input.shouldHaveValue("3025-02-15")
            }

            alreadySent().shouldNotBeChecked()
            dateSent().shouldBeHidden()
            alreadyPaid().shouldNotBeChecked()
            datePaid().shouldBeHidden()

            reportRendering("edit-invoice.load-basic-fields")
        }
    }

    @Test
    fun `should load invoice with general tax`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Hypnotoad Productions")
                val generalTax = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
                val invoice = invoice(
                    customer = customer,
                    title = "Video production services",
                    currency = "USD",
                    amount = 50000,
                    dateIssued = LocalDate.of(3025, 3, 10),
                    dueDate = LocalDate.of(3025, 4, 10),
                    generalTax = generalTax,
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            generalTax {
                input.shouldHaveSelectedValue("VAT")
            }

            reportRendering("edit-invoice.load-with-general-tax")
        }
    }

    @Test
    fun `should load invoice with notes`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                val invoice = invoice(
                    customer = customer,
                    title = "Robot maintenance contract",
                    currency = "USD",
                    amount = 100000,
                    dateIssued = LocalDate.of(3025, 5, 1),
                    dueDate = LocalDate.of(3025, 6, 1),
                    notes = "# Payment Terms\n\nNet 30 days\nLate payment fee: 5%",
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            notes {
                input.shouldHaveValue("# Payment Terms\n\nNet 30 days\nLate payment fee: 5%")
                input.shouldHavePreviewWithHeading("Payment Terms")
            }

            reportRendering("edit-invoice.load-with-notes")
        }
    }

    @Test
    fun `should load invoice without documents`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Slurm Factory")
                val invoice = invoice(
                    customer = customer,
                    title = "Slurm delivery",
                    currency = "USD",
                    amount = 25000,
                    dateIssued = LocalDate.of(3025, 6, 15),
                    dueDate = LocalDate.of(3025, 7, 15),
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            documentsUpload {
                shouldHaveDocuments(DocumentsUpload.EmptyDocument)
            }

            reportRendering("edit-invoice.load-without-documents")
        }
    }

    @Test
    fun `should load invoice with single document`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Planet Express")
                val document = document(
                    workspace = workspace,
                    name = "contract.pdf",
                    storageLocation = "test-contract-location",
                    sizeInBytes = 4096
                )
                val invoice = invoice(
                    customer = customer,
                    title = "Delivery contract",
                    currency = "USD",
                    amount = 30000,
                    dateIssued = LocalDate.of(3025, 7, 1),
                    dueDate = LocalDate.of(3025, 8, 1),
                    status = InvoiceStatus.DRAFT,
                    attachments = setOf(document)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            documentsUpload {
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("contract.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.EmptyDocument
                )
            }

            reportRendering("edit-invoice.load-with-single-document")
        }
    }

    @Test
    fun `should load invoice with multiple documents`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Amphibios 9 Tourism Board")
                val document1 = document(
                    workspace = workspace,
                    name = "agreement.pdf",
                    storageLocation = "test-agreement",
                    sizeInBytes = 2048
                )
                val document2 = document(
                    workspace = workspace,
                    name = "schedule.pdf",
                    storageLocation = "test-schedule",
                    sizeInBytes = 3072
                )
                val document3 = document(
                    workspace = workspace,
                    name = "budget.xlsx",
                    storageLocation = "test-budget",
                    sizeInBytes = 5120
                )
                val invoice = invoice(
                    customer = customer,
                    title = "Tourism marketing campaign",
                    currency = "USD",
                    amount = 75000,
                    dateIssued = LocalDate.of(3025, 8, 1),
                    dueDate = LocalDate.of(3025, 9, 1),
                    status = InvoiceStatus.DRAFT,
                    attachments = setOf(document1, document2, document3)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            documentsUpload {
                // Documents should be sorted alphabetically
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("agreement.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument("budget.xlsx", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument("schedule.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.EmptyDocument
                )
            }

            reportRendering("edit-invoice.load-with-multiple-documents")
        }
    }

    @Test
    fun `should load invoice with sent and paid dates`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Omicron Persei 8 Embassy")
                val invoice = invoice(
                    customer = customer,
                    title = "Diplomatic delivery services",
                    currency = "USD",
                    amount = 50000,
                    dateIssued = LocalDate.of(3025, 9, 1),
                    dueDate = LocalDate.of(3025, 10, 1),
                    dateSent = LocalDate.of(3025, 9, 2),
                    datePaid = LocalDate.of(3025, 9, 15),
                    status = InvoiceStatus.PAID
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            alreadySent().shouldBeChecked()
            dateSent().shouldBeVisible()
            dateSent().input.shouldHaveValue("3025-09-02")

            alreadyPaid().shouldBeChecked()
            datePaid().shouldBeVisible()
            datePaid().input.shouldHaveValue("3025-09-15")

            reportRendering("edit-invoice.load-with-sent-and-paid-dates")
        }
    }

    @Test
    fun `should edit and save invoice with basic fields`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer1 = customer(workspace = workspace, name = "Old Customer")
                val customer2 = customer(workspace = workspace, name = "New Customer")
                val invoice = invoice(
                    customer = customer1,
                    title = "Old title",
                    currency = "USD",
                    amount = 10000,
                    dateIssued = LocalDate.of(3025, 1, 1),
                    dueDate = LocalDate.of(3025, 2, 1),
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            customer { input.selectOption("New Customer") }
            title { input.fill("Updated delivery services") }
            amount { input.fill("250.00") }
            dateIssued { input.fill("3025-01-10") }
            dueDate { input.fill("3025-02-10") }

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
            .shouldBeEntityWithFields(
                Invoice(
                    title = "Updated delivery services",
                    customerId = testData.customer2.id!!,
                    dateIssued = LocalDate.of(3025, 1, 10),
                    dueDate = LocalDate.of(3025, 2, 10),
                    currency = "USD",
                    amount = 25000,
                    status = InvoiceStatus.DRAFT,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Invoice::id,
                    Invoice::version,
                )
            )
    }

    @Test
    fun `should add general tax and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Bender's Big Score LLC")
                val generalTax = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
                val invoice = invoice(
                    customer = customer,
                    title = "Untaxed service",
                    currency = "USD",
                    amount = 20000,
                    dateIssued = LocalDate.of(3025, 3, 1),
                    dueDate = LocalDate.of(3025, 4, 1),
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            generalTax { input.selectOption("VAT") }

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
            .should {
                it.generalTaxId.shouldBe(testData.generalTax.id)
            }
    }

    @Test
    fun `should change general tax and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Robot Arms Apartments")
                val vat = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
                val gst = generalTax(workspace = workspace, title = "GST", rateInBps = 1000)
                val invoice = invoice(
                    customer = customer,
                    title = "Property management",
                    currency = "USD",
                    amount = 30000,
                    dateIssued = LocalDate.of(3025, 5, 1),
                    dueDate = LocalDate.of(3025, 6, 1),
                    generalTax = vat,
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            generalTax { input.selectOption("GST") }

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
            .should {
                it.generalTaxId.shouldBe(testData.gst.id)
            }
    }

    @Test
    fun `should remove general tax and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Central Bureaucracy")
                val generalTax = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
                val invoice = invoice(
                    customer = customer,
                    title = "Form processing",
                    currency = "USD",
                    amount = 40000,
                    dateIssued = LocalDate.of(3025, 6, 1),
                    dueDate = LocalDate.of(3025, 7, 1),
                    generalTax = generalTax,
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            generalTax {
                // Hover to make clear button visible, then clear
                input.shouldHaveClearButton()
                input.clearSelection()
            }

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
            .should {
                it.generalTaxId.shouldBe(null)
            }
    }

    @Test
    fun `should mark as sent and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Luna Park")
                val invoice = invoice(
                    customer = customer,
                    title = "Entertainment services",
                    currency = "USD",
                    amount = 15000,
                    dateIssued = LocalDate.of(3025, 7, 1),
                    dueDate = LocalDate.of(3025, 8, 1),
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            alreadySent().click()
            dateSent().input.fill("3025-07-05")

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
            .should {
                it.dateSent.shouldBe(LocalDate.of(3025, 7, 5))
                it.status.shouldBe(InvoiceStatus.SENT)
            }
    }

    @Test
    fun `should mark as paid and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Applied Cryogenics")
                val invoice = invoice(
                    customer = customer,
                    title = "Freezing services",
                    currency = "USD",
                    amount = 20000,
                    dateIssued = LocalDate.of(3025, 8, 1),
                    dueDate = LocalDate.of(3025, 9, 1),
                    dateSent = LocalDate.of(3025, 8, 2),
                    status = InvoiceStatus.SENT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            alreadyPaid().click()
            datePaid().input.fill("3025-08-15")

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
            .should {
                it.datePaid.shouldBe(LocalDate.of(3025, 8, 15))
                it.status.shouldBe(InvoiceStatus.PAID)
            }
    }

    @Test
    fun `should unmark as sent and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Amazonians United")
                val invoice = invoice(
                    customer = customer,
                    title = "Interplanetary shipping",
                    currency = "USD",
                    amount = 35000,
                    dateIssued = LocalDate.of(3025, 9, 1),
                    dueDate = LocalDate.of(3025, 10, 1),
                    dateSent = LocalDate.of(3025, 9, 2),
                    status = InvoiceStatus.SENT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            alreadySent().click()

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
            .should {
                it.dateSent.shouldBe(null)
                it.status.shouldBe(InvoiceStatus.DRAFT)
            }
    }

    @Test
    fun `should unmark as paid and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Head Museum")
                val invoice = invoice(
                    customer = customer,
                    title = "Tour guide services",
                    currency = "USD",
                    amount = 12000,
                    dateIssued = LocalDate.of(3025, 10, 1),
                    dueDate = LocalDate.of(3025, 11, 1),
                    dateSent = LocalDate.of(3025, 10, 2),
                    datePaid = LocalDate.of(3025, 10, 15),
                    status = InvoiceStatus.PAID
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            alreadyPaid().click()

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
            .should {
                it.datePaid.shouldBe(null)
                it.status.shouldBe(InvoiceStatus.SENT)
            }
    }

    @Test
    fun `should keep existing documents when saving`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Wernstrom's Lab")
                val document1 = document(
                    workspace = workspace,
                    name = "invoice-1.pdf",
                    storageLocation = "test-invoice-1",
                    sizeInBytes = 1024
                )
                val document2 = document(
                    workspace = workspace,
                    name = "invoice-2.pdf",
                    storageLocation = "test-invoice-2",
                    sizeInBytes = 2048
                )
                val invoice = invoice(
                    customer = customer,
                    title = "Scientific consulting",
                    currency = "USD",
                    amount = 45000,
                    dateIssued = LocalDate.of(3025, 11, 1),
                    dueDate = LocalDate.of(3025, 12, 1),
                    status = InvoiceStatus.DRAFT,
                    attachments = setOf(document1, document2)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            title { input.fill("Updated scientific consulting") }

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        val savedInvoice = aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
        savedInvoice.shouldBeEntityWithFields(
            Invoice(
                title = "Updated scientific consulting",
                customerId = testData.customer.id!!,
                dateIssued = LocalDate.of(3025, 11, 1),
                dueDate = LocalDate.of(3025, 12, 1),
                currency = "USD",
                amount = 45000,
                status = InvoiceStatus.DRAFT,
                generalTaxId = null,
            ),
            ignoredProperties = arrayOf(
                Invoice::id,
                Invoice::version,
                Invoice::attachments,
            )
        )
        savedInvoice.attachments.shouldHaveSize(2)

        val documentIds = savedInvoice.attachments.map { it.documentId }.toSet()
        documentIds.shouldBe(setOf(testData.document1.id!!, testData.document2.id!!))
    }

    @Test
    fun `should replace all documents when saving`(page: Page) {
        val file1Content = "New contract 1".toByteArray()
        val file2Content = "New contract 2".toByteArray()

        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Leela's Parents")
                val oldDocument1 = document(
                    workspace = workspace,
                    name = "old-contract-1.pdf",
                    storageLocation = "test-old-1",
                    sizeInBytes = 512
                )
                val oldDocument2 = document(
                    workspace = workspace,
                    name = "old-contract-2.pdf",
                    storageLocation = "test-old-2",
                    sizeInBytes = 1024
                )
                val invoice = invoice(
                    customer = customer,
                    title = "Mutant sewer maintenance",
                    currency = "USD",
                    amount = 8000,
                    dateIssued = LocalDate.of(3025, 12, 1),
                    dueDate = LocalDate.of(3026, 1, 1),
                    status = InvoiceStatus.DRAFT,
                    attachments = setOf(oldDocument1, oldDocument2)
                )
            }
        }

        val testFile1 = createTestFile("new-contract-1.pdf", file1Content)
        val testFile2 = createTestFile("new-contract-2.pdf", file2Content)

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            documentsUpload {
                // Remove old documents
                removeDocument("old-contract-1.pdf")
                removeDocument("old-contract-2.pdf")

                // Upload new documents
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

        page.shouldBeInvoicesOverviewPage()

        val savedInvoice = aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
        savedInvoice.shouldWithClue("Invoice should have two new attachments") {
            attachments.shouldHaveSize(2)
        }

        val documents = savedInvoice.attachments.map { attachment ->
            aggregateTemplate.findSingle<Document>(attachment.documentId)
        }

        val doc1 = documents.find { it.name == testFile1.name }!!
        doc1.shouldWithClue("First document metadata should be correct") {
            this.name.shouldBe(testFile1.name)
            this.sizeInBytes.shouldBe(file1Content.size.toLong())
            this.storageId.shouldBe(TestDocumentsStorage.STORAGE_ID)
            this.workspaceId.shouldBe(testData.workspace.id)
        }
        testDocumentsStorage.getUploadedContent(doc1.storageLocation!!).shouldBe(file1Content)

        val doc2 = documents.find { it.name == testFile2.name }!!
        doc2.shouldWithClue("Second document metadata should be correct") {
            this.name.shouldBe(testFile2.name)
            this.sizeInBytes.shouldBe(file2Content.size.toLong())
            this.storageId.shouldBe(TestDocumentsStorage.STORAGE_ID)
            this.workspaceId.shouldBe(testData.workspace.id)
        }
        testDocumentsStorage.getUploadedContent(doc2.storageLocation!!).shouldBe(file2Content)
    }

    @Test
    fun `should replace subset of documents when saving`(page: Page) {
        val newFileContent = "New delivery schedule".toByteArray()

        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Zapp Brannigan")
                val documentToKeep = document(
                    workspace = workspace,
                    name = "contract.pdf",
                    storageLocation = "test-keep-contract",
                    sizeInBytes = 2048
                )
                val documentToRemove = document(
                    workspace = workspace,
                    name = "old-schedule.pdf",
                    storageLocation = "test-old-schedule",
                    sizeInBytes = 1024
                )
                val invoice = invoice(
                    customer = customer,
                    title = "DOOP supplies delivery",
                    currency = "USD",
                    amount = 55000,
                    dateIssued = LocalDate.of(3026, 1, 1),
                    dueDate = LocalDate.of(3026, 2, 1),
                    status = InvoiceStatus.DRAFT,
                    attachments = setOf(documentToKeep, documentToRemove)
                )
            }
        }

        val testFile = createTestFile("new-schedule.pdf", newFileContent)

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            documentsUpload {
                removeDocument("old-schedule.pdf")
                selectFileForUpload(testFile)

                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("contract.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument(testFile.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )
            }

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        val savedInvoice = aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
        savedInvoice.shouldWithClue("Invoice should have two attachments") {
            attachments.shouldHaveSize(2)
        }

        val documentIds = savedInvoice.attachments.map { it.documentId }.toSet()
        documentIds.shouldWithClue("Should keep old document and add new one") {
            this.contains(testData.documentToKeep.id!!)
            this.shouldHaveSize(2)
        }

        val documents = savedInvoice.attachments.map { attachment ->
            aggregateTemplate.findSingle<Document>(attachment.documentId)
        }

        val newDoc = documents.find { it.name == testFile.name }!!
        newDoc.shouldWithClue("New document should be saved correctly") {
            this.name.shouldBe(testFile.name)
            this.sizeInBytes.shouldBe(newFileContent.size.toLong())
        }
        testDocumentsStorage.getUploadedContent(newDoc.storageLocation!!).shouldBe(newFileContent)
    }

    @Test
    fun `should remove subset of documents when saving`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Kif Kroker")
                val document1 = document(
                    workspace = workspace,
                    name = "agreement.pdf",
                    storageLocation = "test-agreement-keep",
                    sizeInBytes = 3072
                )
                val document2 = document(
                    workspace = workspace,
                    name = "terms-1.pdf",
                    storageLocation = "test-terms-1",
                    sizeInBytes = 1024
                )
                val document3 = document(
                    workspace = workspace,
                    name = "terms-2.pdf",
                    storageLocation = "test-terms-2",
                    sizeInBytes = 2048
                )
                val invoice = invoice(
                    customer = customer,
                    title = "Military logistics support",
                    currency = "USD",
                    amount = 60000,
                    dateIssued = LocalDate.of(3026, 2, 1),
                    dueDate = LocalDate.of(3026, 3, 1),
                    status = InvoiceStatus.DRAFT,
                    attachments = setOf(document1, document2, document3)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            documentsUpload {
                removeDocument("terms-1.pdf")
                removeDocument("terms-2.pdf")

                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("agreement.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.EmptyDocument
                )
            }

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        val savedInvoice = aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
        savedInvoice.shouldWithClue("Invoice should have one remaining attachment") {
            attachments.shouldHaveSize(1)
        }

        val documentIds = savedInvoice.attachments.map { it.documentId }.toSet()
        documentIds.shouldBe(setOf(testData.document1.id!!))
    }

    @Test
    fun `should remove all documents when saving`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Hermes Conrad")
                val document = document(
                    workspace = workspace,
                    name = "old-invoice.pdf",
                    storageLocation = "test-old-invoice",
                    sizeInBytes = 4096
                )
                val invoice = invoice(
                    customer = customer,
                    title = "Bureaucratic filing services",
                    currency = "USD",
                    amount = 5000,
                    dateIssued = LocalDate.of(3026, 3, 1),
                    dueDate = LocalDate.of(3026, 4, 1),
                    status = InvoiceStatus.DRAFT,
                    attachments = setOf(document)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            documentsUpload {
                removeDocument("old-invoice.pdf")

                shouldHaveDocuments(DocumentsUpload.EmptyDocument)
            }

            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        val savedInvoice = aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
        savedInvoice.shouldWithClue("Invoice should have no attachments") {
            attachments.shouldHaveSize(0)
        }
    }

    @Test
    fun `should validate required fields`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Validation Test Corp")
                val invoice = invoice(
                    customer = customer,
                    title = "Test invoice",
                    currency = "USD",
                    amount = 10000,
                    dateIssued = LocalDate.of(3025, 1, 1),
                    dueDate = LocalDate.of(3025, 2, 1),
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            title { input.fill("") }
            amount { input.fill("") }
            dueDate { input.fill("") }

            saveButton.click()

            title {
                shouldHaveValidationError("Please provide the title")
            }
            amount {
                shouldHaveValidationError("Please provide invoice amount")
            }
            dueDate {
                shouldHaveValidationError("Please provide the date when invoice is due")
            }

            reportRendering("edit-invoice.validation-errors")
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Cancelled Test Inc")
                val invoice = invoice(
                    customer = customer,
                    title = "Original title",
                    currency = "USD",
                    amount = 10000,
                    dateIssued = LocalDate.of(3025, 5, 1),
                    dueDate = LocalDate.of(3025, 6, 1),
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            title { input.fill("Modified title that should be discarded") }
            amount { input.fill("999.99") }

            cancelButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
            .shouldWithClue("Invoice should not be modified") {
                title.shouldBe("Original title")
                amount.shouldBe(10000)
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

        fun EditInvoicePage.verifyFieldsVisibility(
            expected: ExpectedFieldsVisibility
        ) {
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

        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "UI States Test Corp")
                val invoice = invoice(
                    customer = customer,
                    title = "UI test invoice",
                    currency = "USD",
                    amount = 10000,
                    dateIssued = LocalDate.of(3025, 1, 1),
                    dueDate = LocalDate.of(3025, 2, 1),
                    status = InvoiceStatus.DRAFT
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/invoices/${testData.invoice.id}/edit")
        page.shouldBeEditInvoicePage {
            var expectedState = ExpectedFieldsVisibility()
            verifyFieldsVisibility(expectedState)

            reportRendering("edit-invoice.initial-state")

            // Enable already sent - date sent field appears
            alreadySent().click()
            expectedState = expectedState.copy(dateSent = true)
            verifyFieldsVisibility(expectedState)

            reportRendering("edit-invoice.already-sent-enabled")

            // Enable already paid - date paid field appears
            alreadyPaid().click()
            expectedState = expectedState.copy(datePaid = true)
            verifyFieldsVisibility(expectedState)

            reportRendering("edit-invoice.already-paid-enabled")

            // Disable already sent - date sent field disappears
            alreadySent().click()
            expectedState = expectedState.copy(dateSent = false)
            verifyFieldsVisibility(expectedState)

            reportRendering("edit-invoice.already-sent-disabled")

            // Disable already paid - date paid field disappears
            alreadyPaid().click()
            expectedState = expectedState.copy(datePaid = false)
            verifyFieldsVisibility(expectedState)

            reportRendering("edit-invoice.already-paid-disabled")
        }
    }

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = Files.createTempFile("test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }
}
