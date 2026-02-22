package io.orangebuffalo.simpleaccounting.business.ui.user.expenses

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.CreateExpensePage.Companion.openCreateExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
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

class CreateExpenseFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should create expense in default currency`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Slurm supplies") }
            originalAmount { input.fill("50.00") }
            datePaid { input.fill("3025-01-15") }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>()
            .shouldBeEntityWithFields(
                Expense(
                    title = "Slurm supplies",
                    categoryId = preconditions.category.id!!,
                    datePaid = LocalDate.of(3025, 1, 15),
                    currency = "USD",
                    originalAmount = 5000,
                    convertedAmounts = AmountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(5000),
                    status = ExpenseStatus.FINALIZED,
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    timeRecorded = MOCK_TIME,
                    workspaceId = preconditions.workspace.id!!,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Expense::id,
                    Expense::version,
                )
            )
    }

    @Test
    fun `should create expense in foreign currency with same tax reporting amount`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Robot oil supplies") }
            currency { input.selectOption("EUREuro") }
            originalAmount { input.fill("100.00") }
            datePaid { input.fill("3025-01-15") }
            convertedAmountInDefaultCurrency("USD").input.fill("110.00")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>()
            .shouldBeEntityWithFields(
                Expense(
                    title = "Robot oil supplies",
                    categoryId = preconditions.category.id!!,
                    datePaid = LocalDate.of(3025, 1, 15),
                    currency = "EUR",
                    originalAmount = 10000,
                    convertedAmounts = AmountsInDefaultCurrency(11000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(11000),
                    status = ExpenseStatus.FINALIZED,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    timeRecorded = MOCK_TIME,
                    workspaceId = preconditions.workspace.id!!,
                    percentOnBusiness = 100,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Expense::id,
                    Expense::version,
                )
            )
    }

    @Test
    fun `should create expense in foreign currency with different tax reporting amount`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Spaceship parts") }
            currency { input.selectOption("GBPBritish Pound") }
            originalAmount { input.fill("80.00") }
            datePaid { input.fill("3025-01-16") }
            convertedAmountInDefaultCurrency("USD").input.fill("100.00")
            useDifferentExchangeRateForIncomeTaxPurposes().click()
            incomeTaxableAmountInDefaultCurrency("USD").input.fill("95.00")
            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>()
            .shouldBeEntityWithFields(
                Expense(
                    title = "Spaceship parts",
                    categoryId = preconditions.category.id!!,
                    datePaid = LocalDate.of(3025, 1, 16),
                    currency = "GBP",
                    originalAmount = 8000,
                    convertedAmounts = AmountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(9500),
                    status = ExpenseStatus.FINALIZED,
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    timeRecorded = MOCK_TIME,
                    workspaceId = preconditions.workspace.id!!,
                    percentOnBusiness = 100,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Expense::id,
                    Expense::version,
                )
            )
    }

    @Test
    fun `should create expense with partial business usage`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Mixed delivery expense") }
            originalAmount { input.fill("100.00") }
            datePaid { input.fill("3025-01-15") }
            partialForBusiness().click()
            percentOnBusiness().input.fill("75")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>()
            .shouldBeEntityWithFields(
                Expense(
                    title = "Mixed delivery expense",
                    categoryId = preconditions.category.id!!,
                    datePaid = LocalDate.of(3025, 1, 15),
                    currency = "USD",
                    originalAmount = 10000,
                    convertedAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 10000,
                        adjustedAmountInDefaultCurrency = 7500
                    ),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 10000,
                        adjustedAmountInDefaultCurrency = 7500
                    ),
                    status = ExpenseStatus.FINALIZED,
                    percentOnBusiness = 75,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    timeRecorded = MOCK_TIME,
                    workspaceId = preconditions.workspace.id!!,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Expense::id,
                    Expense::version,
                )
            )
    }

    @Test
    fun `should create expense with notes`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Intergalactic delivery") }
            originalAmount { input.fill("50.00") }
            datePaid { input.fill("3025-01-15") }
            notes {
                input.fill("# Important Note\n\nExpense notes with **markdown**")
                input.shouldHavePreviewWithHeading("Important Note")
            }

            reportRendering("create-expense.with-notes")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>()
            .should {
                it.notes.shouldBe("# Important Note\n\nExpense notes with **markdown**")
            }
    }

    @Test
    fun `should create expense with document attachments`(page: Page) {
        val file1Content = "Receipt for Slurm supplies".toByteArray()
        val file2Content = "Invoice for office equipment".toByteArray()

        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry).also {
                    category(workspace = it, name = "Delivery")
                }
            }
        }

        val testFile1 = createTestFile("receipt.pdf", file1Content)
        val testFile2 = createTestFile("invoice.pdf", file2Content)

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Oxygen tank refill") }
            originalAmount { input.fill("75.00") }
            datePaid { input.fill("3025-01-15") }

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

            reportRendering("create-expense.with-attachments")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        // Verify expense was created with two attachments
        val savedExpense = aggregateTemplate.findSingle<Expense>()
        savedExpense.shouldWithClue("Expense should have two attachments") {
            attachments.shouldHaveSize(2)
        }

        // Verify both documents were saved with correct metadata and content
        val documents = savedExpense.attachments.map { attachment ->
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
    fun `should create expense with general tax`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Hover scooter maintenance") }
            originalAmount { input.fill("100.00") }
            datePaid { input.fill("3025-01-15") }
            generalTax { input.selectOption("VAT") }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>()
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

            // Verify all required fields show validation errors
            title {
                shouldHaveValidationError("Please provide the title")
            }
            // Date Paid gets a default value from the clock, so it won't have a validation error
            originalAmount {
                shouldHaveValidationError("Please provide expense amount")
            }

            reportRendering("create-expense.validation-errors")

            // Currency should not have error as it has default value
            currency {
                shouldNotHaveValidationErrors()
            }

            // Category doesn't have client-side validation rules

            // Test conditionally rendered fields
            // Switch to foreign currency to show conversion fields
            currency { input.selectOption("EUREuro") }

            // Submit again to trigger validation on conditional fields
            saveButton.click()

            // Conditionally rendered field should be visible and may have validation errors
            convertedAmountInDefaultCurrency("USD").shouldBeVisible()

            // Fill the converted amount
            convertedAmountInDefaultCurrency("USD").input.fill("100.00")

            // Enable different tax rate checkbox
            useDifferentExchangeRateForIncomeTaxPurposes().click()

            // Submit again to check tax amount field validation
            saveButton.click()

            // Tax amount field should now be visible
            incomeTaxableAmountInDefaultCurrency("USD").shouldBeVisible()

            reportRendering("create-expense.validation-errors-with-conditional-fields")
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Cancelled moon trip") }

            cancelButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findAll<Expense>()
            .shouldWithClue("No expenses should be created") {
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
            val datePaid: Boolean = true,
            val convertedAmount: Boolean = false,
            val incomeTaxableAmount: Boolean = false,
            val generalTax: Boolean = true,
            val percentOnBusiness: Boolean = false,
            val notes: Boolean = true,
            val saveButton: Boolean = true,
            val cancelButton: Boolean = true
        )

        fun CreateExpensePage.verifyFieldsVisibility(expected: ExpectedFieldsVisibility) {
            // Always visible basic fields
            if (expected.category) category.shouldBeVisible() else category.shouldBeHidden()
            if (expected.title) title.shouldBeVisible() else title.shouldBeHidden()
            if (expected.currency) currency.shouldBeVisible() else currency.shouldBeHidden()
            if (expected.originalAmount) originalAmount.shouldBeVisible() else originalAmount.shouldBeHidden()
            if (expected.datePaid) datePaid.shouldBeVisible() else datePaid.shouldBeHidden()
            if (expected.generalTax) generalTax.shouldBeVisible() else generalTax.shouldBeHidden()
            if (expected.notes) notes.shouldBeVisible() else notes.shouldBeHidden()
            if (expected.saveButton) saveButton.shouldBeVisible() else saveButton.shouldBeHidden()
            if (expected.cancelButton) cancelButton.shouldBeVisible() else cancelButton.shouldBeHidden()

            // Conditionally visible - foreign currency fields
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

            // Conditionally visible - partial business percentage field
            if (expected.percentOnBusiness) {
                percentOnBusiness().shouldBeVisible()
            } else {
                percentOnBusiness().shouldBeHidden()
            }
        }

        page.setupPreconditionsAndNavigateToCreatePage {
            // Initial state - all basic fields visible, conditional fields hidden
            var expectedState = ExpectedFieldsVisibility()
            verifyFieldsVisibility(expectedState)
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
            }

            reportRendering("create-expense.initial-state")

            // Change to foreign currency - converted amount field appears
            currency { input.selectOption("EUREuro") }
            expectedState = expectedState.copy(convertedAmount = true)
            verifyFieldsVisibility(expectedState)

            reportRendering("create-expense.foreign-currency-fields")

            // Enable different tax rate - income taxable amount field appears
            useDifferentExchangeRateForIncomeTaxPurposes().click()
            expectedState = expectedState.copy(incomeTaxableAmount = true)
            verifyFieldsVisibility(expectedState)

            reportRendering("create-expense.different-tax-rate-enabled")

            // Change back to default currency - foreign currency fields disappear
            currency { input.selectOption("USDUS Dollar") }
            expectedState = expectedState.copy(
                convertedAmount = false,
                incomeTaxableAmount = false
            )
            verifyFieldsVisibility(expectedState)

            reportRendering("create-expense.back-to-default-currency")

            // Enable partial business - percentage field appears
            partialForBusiness().click()
            expectedState = expectedState.copy(percentOnBusiness = true)
            verifyFieldsVisibility(expectedState)
            percentOnBusiness().input.shouldHaveValue("100")

            reportRendering("create-expense.partial-business-enabled")

            // Disable partial business - percentage field disappears
            partialForBusiness().click()
            expectedState = expectedState.copy(percentOnBusiness = false)
            verifyFieldsVisibility(expectedState)

            reportRendering("create-expense.partial-business-disabled")
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
        page.openCreateExpensePage {
            // Verify category dropdown has all categories sorted alphabetically
            category {
                input.shouldHaveOptions("Category A", "Category B", "Category C")
            }

            // Verify general tax dropdown has all taxes
            generalTax {
                input.shouldHaveOptions("GST", "VAT")
            }
        }
    }

    private fun Page.setupPreconditionsAndNavigateToCreatePage(spec: CreateExpensePage.() -> Unit) {
        authenticateViaCookie(preconditions.fry)
        openCreateExpensePage(spec)
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
