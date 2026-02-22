package io.orangebuffalo.simpleaccounting.business.ui.user.incomes

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.CreateIncomePage.Companion.openCreateIncomePage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.IncomesOverviewPage.Companion.shouldBeIncomesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.name
import kotlin.io.path.writeBytes

class CreateIncomeFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should create income in default currency`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Interplanetary delivery fee") }
            originalAmount { input.fill("50.00") }
            dateReceived { input.fill("3025-01-15") }

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>()
            .shouldBeEntityWithFields(
                Income(
                    title = "Interplanetary delivery fee",
                    categoryId = preconditions.category.id!!,
                    dateReceived = LocalDate.of(3025, 1, 15),
                    currency = "USD",
                    originalAmount = 5000,
                    convertedAmounts = AmountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(5000),
                    status = IncomeStatus.FINALIZED,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    timeRecorded = MOCK_TIME,
                    workspaceId = preconditions.workspace.id!!,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Income::id,
                    Income::version,
                )
            )
    }

    @Test
    fun `should create income in foreign currency with same tax reporting amount`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Moon cargo payment") }
            currency { input.selectOption("EUREuro") }
            originalAmount { input.fill("100.00") }
            dateReceived { input.fill("3025-01-15") }
            convertedAmountInDefaultCurrency("USD").input.fill("110.00")

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>()
            .shouldBeEntityWithFields(
                Income(
                    title = "Moon cargo payment",
                    categoryId = preconditions.category.id!!,
                    dateReceived = LocalDate.of(3025, 1, 15),
                    currency = "EUR",
                    originalAmount = 10000,
                    convertedAmounts = AmountsInDefaultCurrency(11000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(11000),
                    status = IncomeStatus.FINALIZED,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    timeRecorded = MOCK_TIME,
                    workspaceId = preconditions.workspace.id!!,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Income::id,
                    Income::version,
                )
            )
    }

    @Test
    fun `should create income in foreign currency with different tax reporting amount`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Robot oil export") }
            currency { input.selectOption("GBPBritish Pound") }
            originalAmount { input.fill("80.00") }
            dateReceived { input.fill("3025-01-16") }
            convertedAmountInDefaultCurrency("USD").input.fill("100.00")
            useDifferentExchangeRateForIncomeTaxPurposes().click()
            incomeTaxableAmountInDefaultCurrency("USD").input.fill("95.00")

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>()
            .shouldBeEntityWithFields(
                Income(
                    title = "Robot oil export",
                    categoryId = preconditions.category.id!!,
                    dateReceived = LocalDate.of(3025, 1, 16),
                    currency = "GBP",
                    originalAmount = 8000,
                    convertedAmounts = AmountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(9500),
                    status = IncomeStatus.FINALIZED,
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    timeRecorded = MOCK_TIME,
                    workspaceId = preconditions.workspace.id!!,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Income::id,
                    Income::version,
                )
            )
    }

    @Test
    fun `should create income in foreign currency without converted amount`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Pending Slurm payment") }
            currency { input.selectOption("EUREuro") }
            originalAmount { input.fill("100.00") }
            dateReceived { input.fill("3025-01-15") }

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>()
            .shouldBeEntityWithFields(
                Income(
                    title = "Pending Slurm payment",
                    categoryId = preconditions.category.id!!,
                    dateReceived = LocalDate.of(3025, 1, 15),
                    currency = "EUR",
                    originalAmount = 10000,
                    convertedAmounts = AmountsInDefaultCurrency(null),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(null),
                    status = IncomeStatus.PENDING_CONVERSION,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    timeRecorded = MOCK_TIME,
                    workspaceId = preconditions.workspace.id!!,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Income::id,
                    Income::version,
                )
            )
    }

    @Test
    fun `should create income in foreign currency with different tax rate but without taxable amount`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Omicron Persei cargo pending tax") }
            currency { input.selectOption("EUREuro") }
            originalAmount { input.fill("80.00") }
            dateReceived { input.fill("3025-01-16") }
            convertedAmountInDefaultCurrency("USD").input.fill("100.00")
            useDifferentExchangeRateForIncomeTaxPurposes().click()

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>()
            .shouldBeEntityWithFields(
                Income(
                    title = "Omicron Persei cargo pending tax",
                    categoryId = preconditions.category.id!!,
                    dateReceived = LocalDate.of(3025, 1, 16),
                    currency = "EUR",
                    originalAmount = 8000,
                    convertedAmounts = AmountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(null),
                    status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    timeRecorded = MOCK_TIME,
                    workspaceId = preconditions.workspace.id!!,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Income::id,
                    Income::version,
                )
            )
    }

    @Test
    fun `should create income with notes`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Delivery to Omicron Persei 8") }
            originalAmount { input.fill("50.00") }
            dateReceived { input.fill("3025-01-15") }
            notes {
                input.fill("# Good News\n\nPayment received for **interplanetary** delivery")
                input.shouldHavePreviewWithHeading("Good News")
            }

            reportRendering("create-income.with-notes")

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>()
            .should {
                it.notes.shouldBe("# Good News\n\nPayment received for **interplanetary** delivery")
            }
    }

    @Test
    fun `should create income with document attachments`(page: Page) {
        val file1Content = "Receipt for Slurm delivery".toByteArray()
        val file2Content = "Confirmation from Mom's Friendly Robot Company".toByteArray()

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry).also {
                    category(workspace = it, name = "Delivery")
                }
            }
        }

        val testFile1 = createTestFile("receipt.pdf", file1Content)
        val testFile2 = createTestFile("confirmation.pdf", file2Content)

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateIncomePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Slurm supplies payment") }
            originalAmount { input.fill("75.00") }
            dateReceived { input.fill("3025-01-15") }

            documentsUpload {
                selectFileForUpload(testFile1)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile1.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )

                selectFileForUpload(testFile2)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument(testFile1.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.UploadedDocument(testFile2.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )
            }

            reportRendering("create-income.with-attachments")

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        val savedIncome = aggregateTemplate.findSingle<Income>()
        savedIncome.shouldWithClue("Income should have two attachments") {
            attachments.shouldHaveSize(2)
        }

        val documents = savedIncome.attachments.map { attachment ->
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

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = Files.createTempFile("test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }

    @Test
    fun `should create income with general tax`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("VAT-inclusive delivery income") }
            originalAmount { input.fill("100.00") }
            dateReceived { input.fill("3025-01-15") }
            generalTax { input.selectOption("VAT") }

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>()
            .should {
                it.generalTaxId.shouldBe(preconditions.generalTax.id)
                it.generalTaxRateInBps.shouldBe(2000)
                // Tax is inclusive: 100.00 with 20% tax means base = 83.33, tax = 16.67
                it.generalTaxAmount.shouldBe(1667)
            }
    }

    @Test
    fun `should validate required fields`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            saveButton.click()

            title {
                shouldHaveValidationError("Please provide the title")
            }
            // Date Received gets a default value from the clock, so it won't have a validation error
            originalAmount {
                shouldHaveValidationError("Please provide income amount")
            }

            reportRendering("create-income.validation-errors")

            // Currency should not have error as it has default value
            currency {
                shouldNotHaveValidationErrors()
            }

            // Switch to foreign currency to show conversion fields
            currency { input.selectOption("EUREuro") }

            // Submit again to trigger validation on conditional fields
            saveButton.click()

            // Conditionally rendered field should be visible
            convertedAmountInDefaultCurrency("USD").shouldBeVisible()

            // Fill the converted amount
            convertedAmountInDefaultCurrency("USD").input.fill("100.00")

            // Enable different exchange rate checkbox
            useDifferentExchangeRateForIncomeTaxPurposes().click()

            // Submit again to check tax amount field validation
            saveButton.click()

            // Tax amount field should now be visible
            incomeTaxableAmountInDefaultCurrency("USD").shouldBeVisible()

            reportRendering("create-income.validation-errors-with-conditional-fields")
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Cancelled delivery payment") }

            cancelButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findAll<Income>()
            .shouldWithClue("No incomes should be created") {
                shouldHaveSize(0)
            }
    }

    @Test
    fun `should handle all UI states correctly`(page: Page) {
        data class ExpectedFieldsVisibility(
            val category: Boolean = true,
            val title: Boolean = true,
            val currency: Boolean = true,
            val originalAmount: Boolean = true,
            val dateReceived: Boolean = true,
            val convertedAmount: Boolean = false,
            val incomeTaxableAmount: Boolean = false,
            val generalTax: Boolean = true,
            val linkedInvoice: Boolean = true,
            val notes: Boolean = true,
            val saveButton: Boolean = true,
            val cancelButton: Boolean = true
        )

        fun CreateIncomePage.verifyFieldsVisibility(expected: ExpectedFieldsVisibility) {
            if (expected.category) category.shouldBeVisible() else category.shouldBeHidden()
            if (expected.title) title.shouldBeVisible() else title.shouldBeHidden()
            if (expected.currency) currency.shouldBeVisible() else currency.shouldBeHidden()
            if (expected.originalAmount) originalAmount.shouldBeVisible() else originalAmount.shouldBeHidden()
            if (expected.dateReceived) dateReceived.shouldBeVisible() else dateReceived.shouldBeHidden()
            if (expected.generalTax) generalTax.shouldBeVisible() else generalTax.shouldBeHidden()
            if (expected.linkedInvoice) linkedInvoice.shouldBeVisible() else linkedInvoice.shouldBeHidden()
            if (expected.notes) notes.shouldBeVisible() else notes.shouldBeHidden()
            if (expected.saveButton) saveButton.shouldBeVisible() else saveButton.shouldBeHidden()
            if (expected.cancelButton) cancelButton.shouldBeVisible() else cancelButton.shouldBeHidden()

            if (expected.convertedAmount) {
                convertedAmountInDefaultCurrency("USD").shouldBeVisible()
            } else {
                convertedAmountInDefaultCurrency("USD").shouldBeHidden()
            }

            if (expected.incomeTaxableAmount) {
                incomeTaxableAmountInDefaultCurrency("USD").shouldBeVisible()
            } else {
                incomeTaxableAmountInDefaultCurrency("USD").shouldBeHidden()
            }
        }

        page.setupPreconditionsAndNavigateToCreatePage {
            // Initial state - all basic fields visible, conditional fields hidden
            var expectedState = ExpectedFieldsVisibility()
            verifyFieldsVisibility(expectedState)
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
            }

            reportRendering("create-income.initial-state")

            // Change to foreign currency - converted amount field appears
            currency { input.selectOption("EUREuro") }
            expectedState = expectedState.copy(convertedAmount = true)
            verifyFieldsVisibility(expectedState)

            reportRendering("create-income.foreign-currency-fields")

            // Enable different exchange rate - income taxable amount field appears
            useDifferentExchangeRateForIncomeTaxPurposes().click()
            expectedState = expectedState.copy(incomeTaxableAmount = true)
            verifyFieldsVisibility(expectedState)

            reportRendering("create-income.different-exchange-rate-enabled")

            // Change back to default currency - foreign currency fields disappear
            currency { input.selectOption("USDUS Dollar") }
            expectedState = expectedState.copy(
                convertedAmount = false,
                incomeTaxableAmount = false
            )
            verifyFieldsVisibility(expectedState)

            reportRendering("create-income.back-to-default-currency")
        }
    }

    @Test
    fun `should display dropdown options correctly`(page: Page) {
        val testPreconditions = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    category(workspace = workspace, name = "Category C")
                    category(workspace = workspace, name = "Category A")
                    category(workspace = workspace, name = "Category B")
                    generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
                    generalTax(workspace = workspace, title = "GST", rateInBps = 1000)
                }
            }
        }

        page.authenticateViaCookie(testPreconditions.fry)
        page.openCreateIncomePage {
            category {
                input.shouldHaveOptions("Category A", "Category B", "Category C")
            }

            generalTax {
                input.shouldHaveOptions("GST", "VAT")
            }
        }
    }

    private fun Page.setupPreconditionsAndNavigateToCreatePage(spec: CreateIncomePage.() -> Unit) {
        authenticateViaCookie(preconditions.fry)
        openCreateIncomePage(spec)
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace, name = "Delivery")
            val generalTax = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
        }
    }
}
