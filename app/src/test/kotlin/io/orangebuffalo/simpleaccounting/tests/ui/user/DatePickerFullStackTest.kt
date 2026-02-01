package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Comprehensive full stack tests for DatePicker component (ElDatePicker).
 * Uses Edit Expense page with Date Paid input as testing grounds.
 */
class DatePickerFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should accept typed date in ISO format`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    datePaid = LocalDate.of(2020, 1, 1)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.fill("2023-12-15")
                input.shouldHaveValue("2023-12-15")
            }
            reportRendering("date-picker.typed-iso-format")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Date should be stored as 2023-12-15") {
                datePaid.shouldBe(LocalDate.of(2023, 12, 15))
            }
    }

    @Test
    fun `should display dates in ISO format regardless of locale`(page: Page) {
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
                    title = "Test",
                    datePaid = LocalDate.of(2023, 7, 25)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.shouldHaveValue("2023-07-25")
            }
            reportRendering("date-picker.de-locale-iso-format")
        }
    }

    @Test
    fun `should accept date selected via popover calendar`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    datePaid = LocalDate.of(2024, 1, 15)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.clickDay(20)
                input.shouldHaveValue("2024-01-20")
            }
            reportRendering("date-picker.popover-selected")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Date should be stored as 2024-01-20") {
                datePaid.shouldBe(LocalDate.of(2024, 1, 20))
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
                    datePaid = LocalDate.of(2023, 7, 25)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.shouldHaveValue("2023-07-25")
            }
            reportRendering("date-picker.prefilled-value")
        }
    }

    @Test
    fun `should handle year boundary dates correctly`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    datePaid = LocalDate.of(2020, 1, 1)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.fill("2023-12-31")
                input.shouldHaveValue("2023-12-31")
            }
            reportRendering("date-picker.year-boundary")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Date should be stored as 2023-12-31") {
                datePaid.shouldBe(LocalDate.of(2023, 12, 31))
            }
    }

    @Test
    fun `should handle leap year date correctly`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    datePaid = LocalDate.of(2020, 1, 1)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.fill("2024-02-29")
                input.shouldHaveValue("2024-02-29")
            }
            reportRendering("date-picker.leap-year")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Date should be stored as 2024-02-29") {
                datePaid.shouldBe(LocalDate.of(2024, 2, 29))
            }
    }

    @Test
    fun `should handle first day of month`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    datePaid = LocalDate.of(2020, 1, 15)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.fill("2023-03-01")
                input.shouldHaveValue("2023-03-01")
            }
            reportRendering("date-picker.first-day-of-month")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Date should be stored as 2023-03-01") {
                datePaid.shouldBe(LocalDate.of(2023, 3, 1))
            }
    }
}

