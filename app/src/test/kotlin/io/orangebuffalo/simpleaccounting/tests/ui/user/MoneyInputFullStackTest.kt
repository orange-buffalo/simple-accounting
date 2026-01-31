package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateExpensePage.Companion.openCreateExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditExpensePage.Companion.shouldBeEditExpensePage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Comprehensive full stack tests for MoneyInput component (SaMoneyInput).
 * Uses Create Expense and Edit Expense pages with Original Amount input as testing grounds.
 */
class MoneyInputFullStackTest : SaFullStackTestBase() {

    @BeforeEach
    fun setup(page: Page) {
        // Resume clock to allow IMask to initialize properly
        page.clock().resume()
    }

    @Test
    fun `should display money input with default currency showing 2 decimal places`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            originalAmount {
                input.shouldBeVisible()
                input.shouldHaveCurrency("USD")
                input.shouldBeEnabled()
            }
            reportRendering("money-input.empty-usd")
        }
    }

    @Test
    fun `should accept input with 2 decimal places for USD`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Test")
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category { input.selectOption("Test") }
            title { input.fill("USD Expense") }
            datePaid { input.fill("2025-01-15") }
            originalAmount {
                input.fill("1234.56")
                input.shouldHaveCurrency("USD")
            }
            reportRendering("money-input.filled-usd-with-separator")

            saveButton.click()
        }

        aggregateTemplate.findAll(Expense::class.java)
            .shouldBeSingle()
            .shouldWithClue("Amount should be stored as 123456 cents") {
                originalAmount.shouldBe(123456)
            }
    }

    @Test
    fun `should accept input with 0 decimal places for JPY`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "JPY")
                val category = category(workspace = workspace, name = "Test")
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category { input.selectOption("Test") }
            title { input.fill("JPY Expense") }
            datePaid { input.fill("2025-01-15") }
            originalAmount {
                input.fill("1234")
                input.shouldHaveCurrency("JPY")
            }
            reportRendering("money-input.filled-jpy-no-decimals")

            saveButton.click()
        }

        aggregateTemplate.findAll(Expense::class.java)
            .shouldBeSingle()
            .shouldWithClue("Amount should be stored as 1234 (no decimal places for JPY)") {
                originalAmount.shouldBe(1234)
            }
    }

    @Test
    fun `should accept input with 3 decimal places for BHD`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "BHD")
                val category = category(workspace = workspace, name = "Test")
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category { input.selectOption("Test") }
            title { input.fill("BHD Expense") }
            datePaid { input.fill("2025-01-15") }
            originalAmount {
                input.fill("123.456")
                input.shouldHaveCurrency("BHD")
            }
            reportRendering("money-input.filled-bhd-3-decimals")

            saveButton.click()
        }

        aggregateTemplate.findAll(Expense::class.java)
            .shouldBeSingle()
            .shouldWithClue("Amount should be stored as 123456 (3 decimal places for BHD)") {
                originalAmount.shouldBe(123456)
            }
    }

    @Test
    fun `should use locale-specific separators for de_DE locale`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    i18nSettings = I18nSettings(locale = "de_DE", language = "en")
                )
                val workspace = workspace(owner = fry, defaultCurrency = "EUR")
                val category = category(workspace = workspace, name = "Test")
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category { input.selectOption("Test") }
            title { input.fill("DE Locale Expense") }
            datePaid { input.fill("2025-01-15") }
            originalAmount {
                // German locale uses . for thousands separator and , for decimal
                input.fill("1234,56")
                input.shouldHaveCurrency("EUR")
            }
            reportRendering("money-input.filled-de-locale")

            saveButton.click()
        }

        aggregateTemplate.findAll(Expense::class.java)
            .shouldBeSingle()
            .shouldWithClue("Amount should be stored correctly despite different separators") {
                originalAmount.shouldBe(123456)
            }
    }

    @Test
    fun `should use locale-specific separators for fr_FR locale`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    i18nSettings = I18nSettings(locale = "fr_FR", language = "en")
                )
                val workspace = workspace(owner = fry, defaultCurrency = "EUR")
                val category = category(workspace = workspace, name = "Test")
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category { input.selectOption("Test") }
            title { input.fill("FR Locale Expense") }
            datePaid { input.fill("2025-01-15") }
            originalAmount {
                // French locale uses space (non-breaking) for thousands separator and , for decimal
                input.fill("9876,54")
                input.shouldHaveCurrency("EUR")
            }
            reportRendering("money-input.filled-fr-locale")

            saveButton.click()
        }

        aggregateTemplate.findAll(Expense::class.java)
            .shouldBeSingle()
            .shouldWithClue("Amount should be stored correctly despite different separators") {
                originalAmount.shouldBe(987654)
            }
    }

    @Test
    fun `should load pre-filled value in edit mode`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Test")
                val expense = expense(
                    workspace = workspace,
                    category = category,
                    title = "Existing Expense",
                    originalAmount = 567890,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            originalAmount {
                input.shouldHaveValue("5,678.90")
                input.shouldHaveCurrency("USD")
            }
            reportRendering("money-input.edit-prefilled")
        }
    }

    @Test
    fun `should handle empty value`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            originalAmount {
                input.shouldHaveValue("")
                input.shouldBeEnabled()
            }
            reportRendering("money-input.empty")
        }
    }

    @Test
    fun `should display currency code for different currencies`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency { input.shouldHaveSelectedValue("USD - US Dollar") }
            originalAmount { input.shouldHaveCurrency("USD") }

            currency { input.selectOption("EUREuro") }
            originalAmount { input.shouldHaveCurrency("EUR") }

            currency { input.selectOption("GBPBritish Pound") }
            originalAmount { input.shouldHaveCurrency("GBP") }

            currency { input.selectOption("JPYJapanese Yen") }
            originalAmount { input.shouldHaveCurrency("JPY") }
            reportRendering("money-input.currency-jpy")
        }
    }

    @Test
    fun `should handle large numbers with thousands separators`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Test")
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category { input.selectOption("Test") }
            title { input.fill("Large Amount") }
            datePaid { input.fill("2025-01-15") }

            originalAmount {
                input.fill("12345.67")
                input.shouldHaveCurrency("USD")
            }
            reportRendering("money-input.large-number")

            saveButton.click()
        }

        aggregateTemplate.findAll(Expense::class.java)
            .shouldBeSingle()
            .shouldWithClue("Large amount should be stored correctly") {
                originalAmount.shouldBe(1234567)
            }
    }

    @Test
    fun `should pad zeros for currency decimal places`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            originalAmount {
                input.fill("100")
                input.shouldHaveCurrency("USD")
            }
            reportRendering("money-input.padded-decimals")
        }
    }
}
