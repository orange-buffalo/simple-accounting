package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.io.path.name
import kotlin.io.path.writeBytes

class EditExpenseFullStackTest : SaFullStackTestBase() {

    @BeforeEach
    fun setup(page: Page) {
        testDocumentsStorage.reset()
        page.clock().resume()
    }

    @Test
    fun `should load expense in default currency`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Office Supplies")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Paper and pens",
                    currency = "USD",
                    originalAmount = 5000,
                    convertedAmounts = AmountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(5000),
                    datePaid = LocalDate.of(2025, 1, 20),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            // Verify general information fields
            category {
                input.shouldHaveSelectedValue("Office Supplies")
            }
            title {
                input.shouldHaveValue("Paper and pens")
            }
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
            }
            originalAmount {
                input.shouldHaveValue("50.00")
            }
            datePaid {
                input.shouldHaveValue("2025-01-20")
            }

            // Verify conditional fields are hidden for default currency
            convertedAmountInDefaultCurrency("USD").shouldBeHidden()
            useDifferentExchangeRateForIncomeTaxPurposes().shouldBeHidden()
            incomeTaxableAmountInDefaultCurrency("USD").shouldBeHidden()

            // Verify partial business checkbox is unchecked and percentage field hidden
            partialForBusiness().shouldNotBeChecked()
            percentOnBusiness().shouldBeHidden()

            reportRendering("edit-expense.load-default-currency")
        }
    }

    @Test
    fun `should load expense in foreign currency with same reporting amount`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Travel")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Hotel in London",
                    currency = "GBP",
                    originalAmount = 12000,
                    convertedAmounts = AmountsInDefaultCurrency(15000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(15000),
                    datePaid = LocalDate.of(2025, 2, 10),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            category {
                input.shouldHaveSelectedValue("Travel")
            }
            title {
                input.shouldHaveValue("Hotel in London")
            }
            currency {
                input.shouldHaveSelectedValue("GBP - British Pound")
            }
            originalAmount {
                input.shouldHaveValue("120.00")
            }
            datePaid {
                input.shouldHaveValue("2025-02-10")
            }

            // Verify foreign currency fields are visible
            convertedAmountInDefaultCurrency("USD").shouldBeVisible()
            convertedAmountInDefaultCurrency("USD").input.shouldHaveValue("150.00")

            // Verify different tax rate checkbox is not checked and income taxable amount is hidden
            useDifferentExchangeRateForIncomeTaxPurposes().shouldNotBeChecked()
            incomeTaxableAmountInDefaultCurrency("USD").shouldBeHidden()

            reportRendering("edit-expense.load-foreign-currency-same-rate")
        }
    }

    @Test
    fun `should load expense in foreign currency with different tax reporting amount`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Equipment")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Computer from Germany",
                    currency = "EUR",
                    originalAmount = 100000,
                    convertedAmounts = AmountsInDefaultCurrency(110000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(105000),
                    datePaid = LocalDate.of(2025, 3, 5),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            category {
                input.shouldHaveSelectedValue("Equipment")
            }
            title {
                input.shouldHaveValue("Computer from Germany")
            }
            currency {
                input.shouldHaveSelectedValue("EUR - Euro")
            }
            originalAmount {
                input.shouldHaveValue("1,000.00")
            }
            datePaid {
                input.shouldHaveValue("2025-03-05")
            }

            // Verify foreign currency fields
            convertedAmountInDefaultCurrency("USD").shouldBeVisible()
            convertedAmountInDefaultCurrency("USD").input.shouldHaveValue("1,100.00")

            // Verify different tax rate is enabled
            useDifferentExchangeRateForIncomeTaxPurposes().shouldBeChecked()
            incomeTaxableAmountInDefaultCurrency("USD").shouldBeVisible()
            incomeTaxableAmountInDefaultCurrency("USD").input.shouldHaveValue("1,050.00")

            reportRendering("edit-expense.load-foreign-currency-different-tax-rate")
        }
    }

    @Test
    fun `should load expense with partial business purpose`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Vehicle")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Car maintenance",
                    currency = "USD",
                    originalAmount = 50000,
                    convertedAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 50000,
                        adjustedAmountInDefaultCurrency = 40000
                    ),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 50000,
                        adjustedAmountInDefaultCurrency = 40000
                    ),
                    datePaid = LocalDate.of(2025, 1, 25),
                    percentOnBusiness = 80,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            category {
                input.shouldHaveSelectedValue("Vehicle")
            }
            title {
                input.shouldHaveValue("Car maintenance")
            }
            originalAmount {
                input.shouldHaveValue("500.00")
            }

            // Verify partial business is enabled
            partialForBusiness().shouldBeChecked()
            percentOnBusiness().shouldBeVisible()
            percentOnBusiness().input.shouldHaveValue("80")

            reportRendering("edit-expense.load-partial-business")
        }
    }

    @Test
    fun `should load expense with general tax`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Services")
                val generalTax = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Consulting services",
                    currency = "USD",
                    originalAmount = 10000,
                    convertedAmounts = AmountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(10000),
                    datePaid = LocalDate.of(2025, 1, 30),
                    percentOnBusiness = 100,
                    generalTax = generalTax,
                    generalTaxRateInBps = 2000,
                    generalTaxAmount = 1667,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            title {
                input.shouldHaveValue("Consulting services")
            }
            generalTax {
                input.shouldHaveSelectedValue("VAT")
            }

            reportRendering("edit-expense.load-with-general-tax")
        }
    }

    @Test
    fun `should load expense with notes`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Training")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Online course",
                    currency = "USD",
                    originalAmount = 20000,
                    convertedAmounts = AmountsInDefaultCurrency(20000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(20000),
                    datePaid = LocalDate.of(2025, 2, 1),
                    percentOnBusiness = 100,
                    notes = "# Course Details\n\nCompleted advanced **programming** course",
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            title {
                input.shouldHaveValue("Online course")
            }
            notes {
                input.shouldHaveValue("# Course Details\n\nCompleted advanced **programming** course")
                input.shouldHavePreviewWithHeading("Course Details")
            }

            reportRendering("edit-expense.load-with-notes")
        }
    }

    @Test
    fun `should load expense without documents`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Subscriptions")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Software subscription",
                    currency = "USD",
                    originalAmount = 9900,
                    convertedAmounts = AmountsInDefaultCurrency(9900),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(9900),
                    datePaid = LocalDate.of(2025, 2, 15),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf()
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            title {
                input.shouldHaveValue("Software subscription")
            }

            documentsUpload {
                shouldHaveDocuments(DocumentsUpload.EmptyDocument)
            }

            reportRendering("edit-expense.load-without-documents")
        }
    }

    @Test
    fun `should load expense with single document`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Utilities")
                val document = document(
                    workspace = workspace,
                    name = "electricity-bill.pdf",
                    storageLocation = "test-location-1",
                    sizeInBytes = 1024
                )
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Electricity bill",
                    currency = "USD",
                    originalAmount = 15000,
                    convertedAmounts = AmountsInDefaultCurrency(15000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(15000),
                    datePaid = LocalDate.of(2025, 2, 20),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf(document)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            title {
                input.shouldHaveValue("Electricity bill")
            }

            documentsUpload {
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("electricity-bill.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.EmptyDocument
                )
            }

            reportRendering("edit-expense.load-with-single-document")
        }
    }

    @Test
    fun `should load expense with multiple documents`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Office")
                val document1 = document(
                    workspace = workspace,
                    name = "receipt-1.pdf",
                    storageLocation = "test-location-1",
                    sizeInBytes = 2048
                )
                val document2 = document(
                    workspace = workspace,
                    name = "receipt-2.pdf",
                    storageLocation = "test-location-2",
                    sizeInBytes = 3072
                )
                val document3 = document(
                    workspace = workspace,
                    name = "invoice.pdf",
                    storageLocation = "test-location-3",
                    sizeInBytes = 4096
                )
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Office supplies bundle",
                    currency = "USD",
                    originalAmount = 30000,
                    convertedAmounts = AmountsInDefaultCurrency(30000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(30000),
                    datePaid = LocalDate.of(2025, 3, 1),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf(document1, document2, document3)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            title {
                input.shouldHaveValue("Office supplies bundle")
            }

            documentsUpload {
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("receipt-1.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument("receipt-2.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument("invoice.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.EmptyDocument
                )
            }

            reportRendering("edit-expense.load-with-multiple-documents")
        }
    }

    @Test
    fun `should edit and save expense in default currency`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category1 = category(workspace = workspace, name = "Office")
                val category2 = category(workspace = workspace, name = "Marketing")
                val expense = expense(
                    workspace = workspace,
                    category = category1,
                    title = "Old title",
                    currency = "USD",
                    originalAmount = 5000,
                    convertedAmounts = AmountsInDefaultCurrency(5000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(5000),
                    datePaid = LocalDate.of(2025, 1, 10),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            category { input.selectOption("Marketing") }
            title { input.fill("Updated office supplies") }
            originalAmount { input.fill("75.50") }
            datePaid { input.fill("2025-01-15") }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .shouldBeEntityWithFields(
                Expense(
                    title = "Updated office supplies",
                    categoryId = testData.category2.id!!,
                    datePaid = LocalDate.of(2025, 1, 15),
                    currency = "USD",
                    originalAmount = 7550,
                    convertedAmounts = AmountsInDefaultCurrency(7550),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(7550),
                    status = ExpenseStatus.FINALIZED,
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    timeRecorded = MOCK_TIME,
                    workspaceId = testData.workspace.id!!,
                    generalTaxId = null,
                ),
                ignoredProperties = arrayOf(
                    Expense::id,
                    Expense::version,
                )
            )
    }

    @Test
    fun `should switch from default to foreign currency and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Travel")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Domestic travel",
                    currency = "USD",
                    originalAmount = 10000,
                    convertedAmounts = AmountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(10000),
                    datePaid = LocalDate.of(2025, 2, 1),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            title { input.fill("International travel") }
            currency { input.selectOption("EUREuro") }
            originalAmount { input.fill("90.00") }
            convertedAmountInDefaultCurrency("USD").input.fill("100.00")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .should {
                it.title.shouldBe("International travel")
                it.currency.shouldBe("EUR")
                it.originalAmount.shouldBe(9000)
                it.convertedAmounts.shouldBe(AmountsInDefaultCurrency(10000))
                it.incomeTaxableAmounts.shouldBe(AmountsInDefaultCurrency(10000))
                it.useDifferentExchangeRateForIncomeTaxPurposes.shouldBe(false)
            }
    }

    @Test
    fun `should switch from foreign to default currency and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Equipment")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Foreign equipment",
                    currency = "GBP",
                    originalAmount = 8000,
                    convertedAmounts = AmountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(10000),
                    datePaid = LocalDate.of(2025, 2, 5),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            title { input.fill("Domestic equipment") }
            currency { input.selectOption("USDUS Dollar") }
            originalAmount { input.fill("125.00") }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .should {
                it.title.shouldBe("Domestic equipment")
                it.currency.shouldBe("USD")
                it.originalAmount.shouldBe(12500)
                it.convertedAmounts.shouldBe(AmountsInDefaultCurrency(12500))
                it.incomeTaxableAmounts.shouldBe(AmountsInDefaultCurrency(12500))
            }
    }

    @Test
    fun `should toggle different tax rate and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Consulting")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Foreign consulting",
                    currency = "EUR",
                    originalAmount = 50000,
                    convertedAmounts = AmountsInDefaultCurrency(55000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(55000),
                    datePaid = LocalDate.of(2025, 2, 10),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            useDifferentExchangeRateForIncomeTaxPurposes().click()
            incomeTaxableAmountInDefaultCurrency("USD").input.fill("520.00")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .should {
                it.useDifferentExchangeRateForIncomeTaxPurposes.shouldBe(true)
                it.convertedAmounts.shouldBe(AmountsInDefaultCurrency(55000))
                it.incomeTaxableAmounts.shouldBe(AmountsInDefaultCurrency(52000))
            }
    }

    @Test
    fun `should change to partial business purpose and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Vehicle")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Full business vehicle",
                    currency = "USD",
                    originalAmount = 30000,
                    convertedAmounts = AmountsInDefaultCurrency(30000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(30000),
                    datePaid = LocalDate.of(2025, 2, 15),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            partialForBusiness().click()
            percentOnBusiness().input.fill("60")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .should {
                it.percentOnBusiness.shouldBe(60)
                it.convertedAmounts.shouldBe(
                    AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 30000,
                        adjustedAmountInDefaultCurrency = 18000
                    )
                )
                it.incomeTaxableAmounts.shouldBe(
                    AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 30000,
                        adjustedAmountInDefaultCurrency = 18000
                    )
                )
            }
    }

    @Test
    fun `should edit notes and save`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Training")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Course fee",
                    currency = "USD",
                    originalAmount = 20000,
                    convertedAmounts = AmountsInDefaultCurrency(20000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(20000),
                    datePaid = LocalDate.of(2025, 2, 20),
                    percentOnBusiness = 100,
                    notes = "Old notes",
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                input.fill("## Updated Notes\n\nCourse completed successfully with **certification**")
                input.shouldHavePreviewWithHeading("Updated Notes")
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .should {
                it.notes.shouldBe("## Updated Notes\n\nCourse completed successfully with **certification**")
            }
    }

    @Test
    fun `should keep existing documents when saving`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Utilities")
                val document1 = document(
                    workspace = workspace,
                    name = "bill-1.pdf",
                    storageLocation = "test-location-1",
                    sizeInBytes = 1024
                )
                val document2 = document(
                    workspace = workspace,
                    name = "bill-2.pdf",
                    storageLocation = "test-location-2",
                    sizeInBytes = 2048
                )
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Utility bills",
                    currency = "USD",
                    originalAmount = 15000,
                    convertedAmounts = AmountsInDefaultCurrency(15000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(15000),
                    datePaid = LocalDate.of(2025, 2, 25),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf(document1, document2)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            title { input.fill("Updated utility bills") }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        val savedExpense = aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
        savedExpense.should {
            it.title.shouldBe("Updated utility bills")
            it.attachments.shouldHaveSize(2)
        }

        val documentIds = savedExpense.attachments.map { it.documentId }.toSet()
        documentIds.shouldBe(setOf(testData.document1.id!!, testData.document2.id!!))
    }

    @Test
    fun `should replace all documents when saving`(page: Page) {
        val file1Content = "New receipt 1".toByteArray()
        val file2Content = "New receipt 2".toByteArray()

        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Office")
                val oldDocument = document(
                    workspace = workspace,
                    name = "old-receipt.pdf",
                    storageLocation = "old-location",
                    sizeInBytes = 512
                )
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Office supplies",
                    currency = "USD",
                    originalAmount = 10000,
                    convertedAmounts = AmountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(10000),
                    datePaid = LocalDate.of(2025, 3, 1),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf(oldDocument)
                )
            }
        }

        val testFile1 = createTestFile("new-receipt-1.pdf", file1Content)
        val testFile2 = createTestFile("new-receipt-2.pdf", file2Content)

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            documentsUpload {
                // Remove old document
                removeDocument("old-receipt.pdf")
                shouldHaveDocuments(DocumentsUpload.EmptyDocument)

                // Upload new documents
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

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        val savedExpense = aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
        savedExpense.should {
            it.attachments.shouldHaveSize(2)
        }

        val documents = savedExpense.attachments.map { attachment ->
            aggregateTemplate.findSingle<Document>(attachment.documentId)
        }

        documents.find { it.name == testFile1.name }!!.shouldWithClue("First new document metadata should be correct") {
            this.name.shouldBe(testFile1.name)
            this.sizeInBytes.shouldBe(file1Content.size.toLong())
        }

        documents.find { it.name == testFile2.name }!!.shouldWithClue("Second new document metadata should be correct") {
            this.name.shouldBe(testFile2.name)
            this.sizeInBytes.shouldBe(file2Content.size.toLong())
        }
    }

    @Test
    fun `should replace subset of documents when saving`(page: Page) {
        val newFileContent = "New receipt content".toByteArray()

        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Marketing")
                val document1 = document(
                    workspace = workspace,
                    name = "old-receipt-1.pdf",
                    storageLocation = "old-location-1",
                    sizeInBytes = 1024
                )
                val document2 = document(
                    workspace = workspace,
                    name = "keep-receipt.pdf",
                    storageLocation = "keep-location",
                    sizeInBytes = 2048
                )
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Marketing materials",
                    currency = "USD",
                    originalAmount = 25000,
                    convertedAmounts = AmountsInDefaultCurrency(25000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(25000),
                    datePaid = LocalDate.of(2025, 3, 5),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf(document1, document2)
                )
            }
        }

        val newTestFile = createTestFile("new-receipt.pdf", newFileContent)

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            documentsUpload {
                // Remove first document, keep second
                removeDocument("old-receipt-1.pdf")
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("keep-receipt.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.EmptyDocument
                )

                // Upload new document
                selectFileForUpload(newTestFile)
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("keep-receipt.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.UploadedDocument(newTestFile.name, DocumentsUpload.DocumentState.PENDING),
                    DocumentsUpload.EmptyDocument
                )
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        val savedExpense = aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
        savedExpense.should {
            it.attachments.shouldHaveSize(2)
        }

        val documentIds = savedExpense.attachments.map { it.documentId }.toSet()
        // Should have the kept document and a new one (not the removed one)
        documentIds.contains(testData.document2.id!!).shouldBe(true)
        documentIds.contains(testData.document1.id!!).shouldBe(false)
    }

    @Test
    fun `should remove subset of documents when saving`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Services")
                val document1 = document(
                    workspace = workspace,
                    name = "invoice-1.pdf",
                    storageLocation = "location-1",
                    sizeInBytes = 1024
                )
                val document2 = document(
                    workspace = workspace,
                    name = "invoice-2.pdf",
                    storageLocation = "location-2",
                    sizeInBytes = 2048
                )
                val document3 = document(
                    workspace = workspace,
                    name = "invoice-3.pdf",
                    storageLocation = "location-3",
                    sizeInBytes = 3072
                )
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Consulting services",
                    currency = "USD",
                    originalAmount = 50000,
                    convertedAmounts = AmountsInDefaultCurrency(50000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(50000),
                    datePaid = LocalDate.of(2025, 3, 10),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf(document1, document2, document3)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            documentsUpload {
                // Remove first and third documents, keep second
                removeDocument("invoice-3.pdf")
                removeDocument("invoice-1.pdf")
                shouldHaveDocuments(
                    DocumentsUpload.UploadedDocument("invoice-2.pdf", DocumentsUpload.DocumentState.COMPLETED),
                    DocumentsUpload.EmptyDocument
                )
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        val savedExpense = aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
        savedExpense.should {
            it.attachments.shouldHaveSize(1)
        }

        val documentIds = savedExpense.attachments.map { it.documentId }.toSet()
        documentIds.shouldBe(setOf(testData.document2.id!!))
    }

    @Test
    fun `should remove all documents when saving`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "test-storage")
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Subscriptions")
                val document = document(
                    workspace = workspace,
                    name = "subscription-invoice.pdf",
                    storageLocation = "sub-location",
                    sizeInBytes = 1024
                )
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Annual subscription",
                    currency = "USD",
                    originalAmount = 99900,
                    convertedAmounts = AmountsInDefaultCurrency(99900),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(99900),
                    datePaid = LocalDate.of(2025, 3, 15),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    attachments = setOf(document)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            documentsUpload {
                removeDocument("subscription-invoice.pdf")
                shouldHaveDocuments(DocumentsUpload.EmptyDocument)
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .should {
                it.attachments.shouldHaveSize(0)
            }
    }

    @Test
    fun `should validate required fields`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Utilities")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Test expense",
                    currency = "USD",
                    originalAmount = 10000,
                    convertedAmounts = AmountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(10000),
                    datePaid = LocalDate.of(2025, 1, 15),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            // Clear required fields
            title { input.fill("") }
            originalAmount { input.fill("") }

            saveButton.click()

            // Verify validation errors
            title {
                shouldHaveValidationError("Please provide the title")
            }
            originalAmount {
                shouldHaveValidationError("Please provide expense amount")
            }

            reportRendering("edit-expense.validation-errors")
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Office")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Original title",
                    currency = "USD",
                    originalAmount = 10000,
                    convertedAmounts = AmountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(10000),
                    datePaid = LocalDate.of(2025, 1, 20),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")
        page.shouldBeEditExpensePage {
            title { input.fill("This will be cancelled") }

            cancelButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        // Verify expense was not modified
        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .should {
                it.title.shouldBe("Original title")
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

        fun verifyFieldsVisibility(expected: ExpectedFieldsVisibility) {
            page.shouldBeEditExpensePage {
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
        }

        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Test")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Test expense",
                    currency = "USD",
                    originalAmount = 10000,
                    convertedAmounts = AmountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(10000),
                    datePaid = LocalDate.of(2025, 1, 15),
                    percentOnBusiness = 100,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/expenses/${testData.expense.id}/edit")

        // Initial state - all basic fields visible, conditional fields hidden
        var expectedState = ExpectedFieldsVisibility()
        verifyFieldsVisibility(expectedState)

        page.shouldBeEditExpensePage {
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
            }
            reportRendering("edit-expense.initial-state")

            // Change to foreign currency - converted amount field appears
            currency { input.selectOption("EUREuro") }
        }
        expectedState = expectedState.copy(convertedAmount = true)
        verifyFieldsVisibility(expectedState)
        page.shouldBeEditExpensePage {
            reportRendering("edit-expense.foreign-currency-fields")

            // Enable different tax rate - income taxable amount field appears
            useDifferentExchangeRateForIncomeTaxPurposes().click()
        }
        expectedState = expectedState.copy(incomeTaxableAmount = true)
        verifyFieldsVisibility(expectedState)
        page.shouldBeEditExpensePage {
            reportRendering("edit-expense.different-tax-rate-enabled")

            // Change back to default currency - foreign currency fields disappear
            currency { input.selectOption("USDUS Dollar") }
        }
        expectedState = expectedState.copy(
            convertedAmount = false,
            incomeTaxableAmount = false
        )
        verifyFieldsVisibility(expectedState)
        page.shouldBeEditExpensePage {
            reportRendering("edit-expense.back-to-default-currency")

            // Enable partial business - percentage field appears
            partialForBusiness().click()
        }
        expectedState = expectedState.copy(percentOnBusiness = true)
        verifyFieldsVisibility(expectedState)
        page.shouldBeEditExpensePage {
            percentOnBusiness().input.shouldHaveValue("100")
            reportRendering("edit-expense.partial-business-enabled")

            // Disable partial business - percentage field disappears
            partialForBusiness().click()
        }
        expectedState = expectedState.copy(percentOnBusiness = false)
        verifyFieldsVisibility(expectedState)
        page.shouldBeEditExpensePage {
            reportRendering("edit-expense.partial-business-disabled")
        }
    }

    private fun createTestFile(fileName: String, content: ByteArray): java.nio.file.Path {
        val testFile = java.nio.file.Files.createTempFile("test-upload-", "-$fileName")
        testFile.writeBytes(content)
        return testFile
    }
}
