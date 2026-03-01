package io.orangebuffalo.simpleaccounting.business.ui.user.incometaxpayments

import com.microsoft.playwright.Page
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPayment
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.CreateIncomeTaxPaymentPage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.CreateIncomeTaxPaymentPage.Companion.openCreateIncomeTaxPaymentPage
import io.orangebuffalo.simpleaccounting.business.ui.user.incometaxpayments.IncomeTaxPaymentsOverviewPage.Companion.shouldBeIncomeTaxPaymentsOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.name
import kotlin.io.path.writeBytes

class CreateIncomeTaxPaymentFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should create income tax payment with required fields only`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            title { input.fill("Q1 Income Tax") }
            amount { input.fill("1500.00") }
            datePaid { input.fill("3025-01-15") }
            reportingDate { input.fill("3025-01-15") }

            saveButton.click()
        }

        page.shouldBeIncomeTaxPaymentsOverviewPage()

        aggregateTemplate.findSingle<IncomeTaxPayment>()
            .shouldBeEntityWithFields(
                IncomeTaxPayment(
                    title = "Q1 Income Tax",
                    amount = 150000,
                    datePaid = LocalDate.of(3025, 1, 15),
                    reportingDate = LocalDate.of(3025, 1, 15),
                    workspaceId = preconditions.workspace.id!!,
                )
            )
    }

    @Test
    fun `should create income tax payment with notes`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            title { input.fill("Annual Tax Payment") }
            amount { input.fill("5000.00") }
            datePaid { input.fill("3025-02-20") }
            reportingDate { input.fill("3025-02-15") }
            notes {
                input.fill("# Important\nTax payment for fiscal year 3024")
                input.shouldHavePreviewWithHeading("Important")
            }

            saveButton.click()
        }

        page.shouldBeIncomeTaxPaymentsOverviewPage()

        aggregateTemplate.findSingle<IncomeTaxPayment>()
            .shouldBeEntityWithFields(
                IncomeTaxPayment(
                    title = "Annual Tax Payment",
                    amount = 500000,
                    datePaid = LocalDate.of(3025, 2, 20),
                    reportingDate = LocalDate.of(3025, 2, 15),
                    notes = "# Important\nTax payment for fiscal year 3024",
                    workspaceId = preconditions.workspace.id!!,
                )
            )
    }

    @Test
    fun `should create income tax payment with attachments`(page: Page) {
        val testFile = createTestFile("tax-receipt.pdf", byteArrayOf(1, 2, 3))

        page.setupPreconditionsAndNavigateToCreatePage {
            title { input.fill("Q2 Tax Payment") }
            amount { input.fill("2000.00") }
            datePaid { input.fill("3025-04-15") }
            reportingDate { input.fill("3025-04-10") }

            documentsUpload {
                selectFileForUpload(testFile)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )
            }

            saveButton.click()
        }

        page.shouldBeIncomeTaxPaymentsOverviewPage()

        val uploadedDocument = aggregateTemplate.findSingle<Document>()
        uploadedDocument.name.shouldBe(testFile.name)

        aggregateTemplate.findSingle<IncomeTaxPayment>().should {
            it.shouldBeEntityWithFields(
                IncomeTaxPayment(
                    title = "Q2 Tax Payment",
                    amount = 200000,
                    datePaid = LocalDate.of(3025, 4, 15),
                    reportingDate = LocalDate.of(3025, 4, 10),
                    workspaceId = preconditions.workspace.id!!,
                ),
                ignoredProperties = arrayOf(
                    IncomeTaxPayment::attachments,
                )
            )
            it.attachments.map { attachment -> attachment.documentId } shouldBe listOf(uploadedDocument.id)
        }
    }

    @Test
    fun `should create income tax payment with all fields`(page: Page) {
        val testFile = createTestFile("comprehensive-receipt.pdf", byteArrayOf(1, 2, 3, 4, 5))

        page.setupPreconditionsAndNavigateToCreatePage {
            title { input.fill("Annual Corporate Tax") }
            amount { input.fill("10000.00") }
            datePaid { input.fill("3025-12-31") }
            reportingDate { input.fill("3025-12-15") }
            notes {
                input.fill("# Corporate Tax\nFull year payment for Planet Express Inc")
                input.shouldHavePreviewWithHeading("Corporate Tax")
            }

            documentsUpload {
                selectFileForUpload(testFile)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )
            }

            saveButton.click()
        }

        page.shouldBeIncomeTaxPaymentsOverviewPage()

        val uploadedDocument = aggregateTemplate.findSingle<Document>()

        aggregateTemplate.findSingle<IncomeTaxPayment>().should {
            it.shouldBeEntityWithFields(
                IncomeTaxPayment(
                    title = "Annual Corporate Tax",
                    amount = 1000000,
                    datePaid = LocalDate.of(3025, 12, 31),
                    reportingDate = LocalDate.of(3025, 12, 15),
                    notes = "# Corporate Tax\nFull year payment for Planet Express Inc",
                    workspaceId = preconditions.workspace.id!!,
                ),
                ignoredProperties = arrayOf(
                    IncomeTaxPayment::attachments,
                )
            )
            it.attachments.map { attachment -> attachment.documentId } shouldBe listOf(uploadedDocument.id)
        }
    }

    private fun Page.setupPreconditionsAndNavigateToCreatePage(
        spec: CreateIncomeTaxPaymentPage.() -> Unit
    ) {
        authenticateViaCookie(preconditions.fry)
        openCreateIncomeTaxPaymentPage {
            spec()
        }
    }

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = Files.createTempFile("test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(
                userName = "Fry",
                isAdmin = false,
                activated = true,
                documentsStorage = TestDocumentsStorage.STORAGE_ID
            )
            val workspace = workspace(owner = fry)
        }
    }
}
