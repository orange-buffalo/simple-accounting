package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import org.junit.jupiter.api.Test

/**
 * Comprehensive full stack tests for MoneyInput component (SaMoneyInput).
 * Uses Edit Expense page with Original Amount input as testing grounds.
 */
class SaMoneyInputFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should accept input with 2 decimal places for USD`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Delivery run",
                    originalAmount = 0,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            originalAmount {
                input.fill("1234.56")
                input.shouldHaveValue("1,234.56")
                input.shouldHaveCurrency("USD")
            }
            reportRendering("money-input.filled-usd-with-separator")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
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
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Delivery run",
                    originalAmount = 0,
                    currency = "JPY"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            originalAmount {
                input.fill("1234")
                input.shouldHaveValue("1,234")
                input.shouldHaveCurrency("JPY")
            }
            reportRendering("money-input.filled-jpy-no-decimals")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
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
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Delivery run",
                    originalAmount = 0,
                    currency = "BHD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            originalAmount {
                input.fill("123.456")
                input.shouldHaveValue("123.456")
                input.shouldHaveCurrency("BHD")
            }
            reportRendering("money-input.filled-bhd-3-decimals")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
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
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Delivery run",
                    originalAmount = 0,
                    currency = "EUR"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            originalAmount {
                // German locale uses . for thousands separator and , for decimal
                input.fill("1234,56")
                input.shouldHaveValue("1.234,56")
                input.shouldHaveCurrency("EUR")
            }
            reportRendering("money-input.filled-de-locale")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
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
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Delivery run",
                    originalAmount = 0,
                    currency = "EUR"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            originalAmount {
                // French locale uses space (non-breaking) for thousands separator and , for decimal
                input.fill("9876,54")
                input.shouldHaveCurrency("EUR")
            }
            reportRendering("money-input.filled-fr-locale")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
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
                val expense = expense(
                    workspace = workspace,
                    category = null,
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
        }
    }

    @Test
    fun `should handle large numbers with thousands separators`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Delivery run",
                    originalAmount = 0,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            originalAmount {
                input.fill("12345.67")
                input.shouldHaveValue("12,345.67")
                input.shouldHaveCurrency("USD")
            }
            reportRendering("money-input.large-number")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
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
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Delivery run",
                    originalAmount = 0,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            originalAmount {
                input.fill("100")
                input.shouldHaveValue("100.00")
                input.shouldHaveCurrency("USD")
            }
            reportRendering("money-input.padded-decimals")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Amount with padded zeros should be stored correctly") {
                originalAmount.shouldBe(10000)
            }
    }
}
