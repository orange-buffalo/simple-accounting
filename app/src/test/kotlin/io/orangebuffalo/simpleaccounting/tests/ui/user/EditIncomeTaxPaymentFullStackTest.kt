package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPayment
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditIncomeTaxPaymentPage.Companion.shouldBeEditIncomeTaxPaymentPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.IncomeTaxPaymentsOverviewPage.Companion.shouldBeIncomeTaxPaymentsOverviewPage
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.io.path.name
import kotlin.io.path.writeBytes

class EditIncomeTaxPaymentFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should load income tax payment with basic fields`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val payment = incomeTaxPayment(
                    workspace = workspace,
                    title = "Q1 Tax Payment",
                    amount = 125000,
                    datePaid = LocalDate.of(3025, 3, 15),
                    reportingDate = LocalDate.of(3025, 3, 10)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/income-tax-payments/${testData.payment.id}/edit")
        page.shouldBeEditIncomeTaxPaymentPage {
            title {
                input.shouldHaveValue("Q1 Tax Payment")
            }
            amount {
                input.shouldHaveValue("1250.00")
            }
            datePaid {
                input.shouldHaveValue("3025-03-15")
            }
            reportingDate {
                input.shouldHaveValue("3025-03-10")
            }
            notes {
                input.shouldHaveValue("")
            }

            reportRendering("edit-income-tax-payment.load-basic")
        }
    }

    @Test
    fun `should load income tax payment with all fields`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    isAdmin = false,
                    activated = true,
                    documentsStorage = "noop"
                )
                val workspace = workspace(owner = fry)
                val document1 = document(workspace = workspace, name = "Tax Receipt 1")
                val document2 = document(workspace = workspace, name = "Tax Receipt 2")
                val payment = incomeTaxPayment(
                    workspace = workspace,
                    title = "Annual Corporate Tax",
                    amount = 500000,
                    datePaid = LocalDate.of(3025, 12, 31),
                    reportingDate = LocalDate.of(3025, 12, 15),
                    notes = "# Corporate Tax\nFull year payment",
                    attachments = setOf(document1, document2)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/income-tax-payments/${testData.payment.id}/edit")
        page.shouldBeEditIncomeTaxPaymentPage {
            title {
                input.shouldHaveValue("Annual Corporate Tax")
            }
            amount {
                input.shouldHaveValue("5000.00")
            }
            datePaid {
                input.shouldHaveValue("3025-12-31")
            }
            reportingDate {
                input.shouldHaveValue("3025-12-15")
            }
            notes {
                input.shouldHaveValue("# Corporate Tax\nFull year payment")
                input.shouldHavePreviewWithHeading("Corporate Tax")
            }

            documentsUpload.shouldHaveDocuments(
                DocumentsUpload.UploadedDocument("Tax Receipt 1", DocumentsUpload.DocumentState.COMPLETED),
                DocumentsUpload.UploadedDocument("Tax Receipt 2", DocumentsUpload.DocumentState.COMPLETED),
                DocumentsUpload.EmptyDocument
            )

            reportRendering("edit-income-tax-payment.load-all-fields")
        }
    }

    @Test
    fun `should update income tax payment basic fields`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val payment = incomeTaxPayment(
                    workspace = workspace,
                    title = "Q1 Tax",
                    amount = 100000,
                    datePaid = LocalDate.of(3025, 1, 15),
                    reportingDate = LocalDate.of(3025, 1, 15)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/income-tax-payments/${testData.payment.id}/edit")
        page.shouldBeEditIncomeTaxPaymentPage {
            title { input.fill("Q1 Corporate Tax") }
            amount { input.fill("2500.00") }
            datePaid { input.fill("3025-01-20") }
            reportingDate { input.fill("3025-01-18") }

            saveButton.click()
        }

        page.shouldBeIncomeTaxPaymentsOverviewPage()

        aggregateTemplate.findSingle<IncomeTaxPayment>(testData.payment.id!!)
            .shouldBeEntityWithFields(
                IncomeTaxPayment(
                    title = "Q1 Corporate Tax",
                    amount = 250000,
                    datePaid = LocalDate.of(3025, 1, 20),
                    reportingDate = LocalDate.of(3025, 1, 18),
                    timeRecorded = MOCK_TIME,
                    workspaceId = testData.workspace.id!!,
                ),
                ignoredProperties = arrayOf(
                    IncomeTaxPayment::id,
                    IncomeTaxPayment::version,
                )
            )
    }

    @Test
    fun `should update income tax payment with notes and attachments`(page: Page) {
        val testFile = createTestFile("updated-receipt.pdf", byteArrayOf(5, 6, 7, 8))

        val testData = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    isAdmin = false,
                    activated = true,
                    documentsStorage = TestDocumentsStorage.STORAGE_ID
                )
                val workspace = workspace(owner = fry)
                val existingDocument = document(workspace = workspace, name = "Old Receipt")
                val payment = incomeTaxPayment(
                    workspace = workspace,
                    title = "Q2 Tax",
                    amount = 150000,
                    datePaid = LocalDate.of(3025, 4, 15),
                    reportingDate = LocalDate.of(3025, 4, 10),
                    attachments = setOf(existingDocument)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/income-tax-payments/${testData.payment.id}/edit")
        page.shouldBeEditIncomeTaxPaymentPage {
            documentsUpload.shouldHaveDocuments(
                DocumentsUpload.UploadedDocument("Old Receipt", DocumentsUpload.DocumentState.COMPLETED),
                DocumentsUpload.EmptyDocument
            )

            notes {
                input.fill("# Updated\nAdding payment notes")
                input.shouldHavePreviewWithHeading("Updated")
            }

            documentsUpload {
                selectFileForUpload(testFile)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("Old Receipt", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument(testFile.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )
            }

            saveButton.click()
        }

        page.shouldBeIncomeTaxPaymentsOverviewPage()

        // Find the new uploaded document (not the existing one)
        val allDocuments = aggregateTemplate.findAll<Document>().toList()
        val uploadedDocument = allDocuments.first { it.name == testFile.name }
        uploadedDocument.name.shouldBe(testFile.name)

        aggregateTemplate.findSingle<IncomeTaxPayment>(testData.payment.id!!).should {
            it.shouldBeEntityWithFields(
                IncomeTaxPayment(
                    title = "Q2 Tax",
                    amount = 150000,
                    datePaid = LocalDate.of(3025, 4, 15),
                    reportingDate = LocalDate.of(3025, 4, 10),
                    notes = "# Updated\nAdding payment notes",
                    timeRecorded = MOCK_TIME,
                    workspaceId = testData.workspace.id!!,
                ),
                ignoredProperties = arrayOf(
                    IncomeTaxPayment::id,
                    IncomeTaxPayment::version,
                    IncomeTaxPayment::attachments,
                )
            )
            val documentIds = it.attachments.map { attachment -> attachment.documentId }.toSet()
            documentIds.shouldBe(setOf(uploadedDocument.id!!, testData.existingDocument.id!!))
        }
    }

    private fun createTestFile(fileName: String, content: ByteArray): java.nio.file.Path {
        val testFile = java.nio.file.Files.createTempFile("test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }
}
