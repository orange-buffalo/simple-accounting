package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateExpensePage.Companion.openCreateExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateExpensePage.Companion.shouldBeCreateExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class CreateExpenseFullStackTest : SaFullStackTestBase() {

    @BeforeEach
    fun setup(page: Page) {
        mockCurrentTime(timeService)
        // Keep clock paused by default for deterministic tests
        // Advance clock when needed for specific interactions (e.g., debouncing)
    }

    @Test
    fun `should create expense in default currency`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Coffee supplies") }
            originalAmount { input.fill("50.00") }
            // Advance clock past IMask debounce timeout
            page.clock().runFor(400)
            datePaid { input.fill("2025-01-15") }

            reportRendering("create-expense.default-currency-filled")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findAll(Expense::class.java)
            .shouldWithClue("Expected exactly one expense created") {
                shouldHaveSize(1)
            }
            .shouldBeSingle()
            .shouldBeEntityWithFields(
                Expense(
                    title = "Coffee supplies",
                    categoryId = preconditions.category.id!!,
                    datePaid = LocalDate.of(2025, 1, 15),
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
            title { input.fill("EUR supplies") }
            currency { input.selectOption("EUREuro") }
            originalAmount { input.fill("100.00") }
            page.clock().runFor(400)
            datePaid { input.fill("2025-01-15") }

            convertedAmountInDefaultCurrency("USD").input.fill("110.00")
            page.clock().runFor(400)

            reportRendering("create-expense.foreign-currency-same-amounts")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findAll(Expense::class.java)
            .shouldBeSingle()
            .shouldBeEntityWithFields(
                Expense(
                    title = "EUR supplies",
                    categoryId = preconditions.category.id!!,
                    datePaid = LocalDate.of(2025, 1, 15),
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
            title { input.fill("GBP supplies") }
            currency { input.selectOption("GBPBritish Pound") }
            originalAmount { input.fill("80.00") }
            page.clock().runFor(400)
            datePaid { input.fill("2025-01-16") }

            convertedAmountInDefaultCurrency("USD").input.fill("100.00")
            page.clock().runFor(400)

            useDifferentExchangeRateForIncomeTaxPurposes().click()

            incomeTaxableAmountInDefaultCurrency("USD").input.fill("95.00")
            page.clock().runFor(400)

            reportRendering("create-expense.foreign-currency-different-amounts")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findAll(Expense::class.java)
            .shouldBeSingle()
            .shouldBeEntityWithFields(
                Expense(
                    title = "GBP supplies",
                    categoryId = preconditions.category.id!!,
                    datePaid = LocalDate.of(2025, 1, 16),
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
            title { input.fill("Mixed purpose expense") }
            originalAmount { input.fill("100.00") }
            page.clock().runFor(400)
            datePaid { input.fill("2025-01-15") }

            partialForBusiness().click()

            percentOnBusiness().input.fill("75")

            reportRendering("create-expense.partial-business")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findAll(Expense::class.java)
            .shouldBeSingle()
            .should {
                it.percentOnBusiness.shouldBe(75)
            }
    }

    @Test
    fun `should create expense with notes`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Expense with notes") }
            originalAmount { input.fill("50.00") }
            page.clock().runFor(400)
            datePaid { input.fill("2025-01-15") }

            notes {
                input.fill("Important expense notes with **markdown**")
                // Advance clock past markdown preview debounce
                page.clock().runFor(400)
                input.shouldHavePreview()
            }

            reportRendering("create-expense.with-notes")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findAll(Expense::class.java)
            .shouldBeSingle()
            .should {
                it.notes.shouldBe("Important expense notes with **markdown**")
            }
    }

    @Test
    fun `should create expense with general tax`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Taxable expense") }
            originalAmount { input.fill("100.00") }
            page.clock().runFor(400)
            datePaid { input.fill("2025-01-15") }

            generalTax { input.selectOption("VAT") }

            reportRendering("create-expense.with-tax")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findAll(Expense::class.java)
            .shouldBeSingle()
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
            reportRendering("create-expense.empty-form")

            saveButton.click()
            
            // Wait a moment for validation to kick in after resuming clock
            page.clock().runFor(100)

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
            
            // Wait a moment for validation
            page.clock().runFor(100)
            
            // Conditionally rendered field should be visible and may have validation errors
            convertedAmountInDefaultCurrency("USD").shouldBeVisible()
            
            // Fill the converted amount
            convertedAmountInDefaultCurrency("USD").input.fill("100.00")
            
            // Enable different tax rate checkbox
            useDifferentExchangeRateForIncomeTaxPurposes().click()
            
            // Submit again to check tax amount field validation
            saveButton.click()
            
            // Wait a moment for validation
            page.clock().runFor(100)
            
            // Tax amount field should now be visible
            incomeTaxableAmountInDefaultCurrency("USD").shouldBeVisible()
            
            reportRendering("create-expense.validation-errors-with-conditional-fields")
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            category { input.selectOption("Delivery") }
            title { input.fill("Will be cancelled") }

            cancelButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findAll(Expense::class.java)
            .shouldWithClue("No expenses should be created") {
                shouldHaveSize(0)
            }
    }

    @Test
    fun `should handle all UI states correctly`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {
            // Verify initial state with default currency
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
            }

            reportRendering("create-expense.initial-state")

            // Change currency to trigger conditional fields
            currency { input.selectOption("EUREuro") }

            // Verify foreign currency fields appear
            convertedAmountInDefaultCurrency("USD").shouldBeVisible()

            reportRendering("create-expense.foreign-currency-fields-visible")

            // Enable different tax rate checkbox
            useDifferentExchangeRateForIncomeTaxPurposes().click()

            // Verify tax amount field appears
            incomeTaxableAmountInDefaultCurrency("USD").shouldBeVisible()

            reportRendering("create-expense.tax-rate-field-visible")

            // Change back to default currency
            currency { input.selectOption("USDUS Dollar") }

            // Verify foreign currency fields are hidden
            convertedAmountInDefaultCurrency("USD").shouldBeHidden()

            reportRendering("create-expense.back-to-default-currency")

            // Enable partial business checkbox
            partialForBusiness().click()

            // Verify percentage field appears
            percentOnBusiness().shouldBeVisible()
            percentOnBusiness().input.shouldHaveValue("100")

            reportRendering("create-expense.partial-business-visible")
        }
    }

    @Test
    fun `should display dropdown options correctly`(page: Page) {
        page.setupPreconditionsForDropdownsAndNavigateToCreatePage {
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

    private fun Page.setupPreconditionsForDropdownsAndNavigateToCreatePage(spec: CreateExpensePage.() -> Unit) {
        authenticateViaCookie(preconditionsDropdowns.fry)
        openCreateExpensePage(spec)
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(
                userName = "Fry",
                isAdmin = false,
                activated = true,
                documentsStorage = "noop"
            )
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace, name = "Delivery")
            val generalTax = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
        }
    }

    private val preconditionsDropdowns by lazyPreconditions {
        object {
            val fry = platformUser(
                userName = "Fry",
                isAdmin = false,
                activated = true,
                documentsStorage = "noop"
            )
            val workspace = workspace(owner = fry)
            
            // Create multiple categories to test sorting
            val categoryC = category(workspace = workspace, name = "Category C")
            val categoryA = category(workspace = workspace, name = "Category A")
            val categoryB = category(workspace = workspace, name = "Category B")
            
            // Create multiple taxes
            val vat = generalTax(workspace = workspace, title = "VAT", rateInBps = 2000)
            val gst = generalTax(workspace = workspace, title = "GST", rateInBps = 1000)
        }
    }
}
