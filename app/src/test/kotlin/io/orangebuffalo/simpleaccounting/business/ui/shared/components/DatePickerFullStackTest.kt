package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.CreateExpensePage.Companion.shouldBeCreateExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage.Companion.assumeEditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import org.junit.jupiter.api.Test
import org.springframework.data.jdbc.core.findAll
import java.time.LocalDate

/**
 * Comprehensive full stack tests for DatePicker component (ElDatePicker).
 * Uses Edit Expense page with Date Paid input as testing grounds.
 */
class DatePickerFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should accept typed date in the user's format`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    datePaid = LocalDate.of(3020, 1, 1)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.fill("15/12/3023")
                input.shouldHaveValue("15/12/3023")
            }
            reportRendering("date-picker.typed-localized-format")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Date should be stored as 2023-12-15") {
                datePaid.shouldBe(LocalDate.of(3023, 12, 15))
            }
    }

    @Test
    fun `should display dates in the selected locale format`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    i18nSettings = I18nSettings(locale = "en_US", language = "en")
                )
                val workspace = workspace(owner = fry, defaultCurrency = "EUR")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    datePaid = LocalDate.of(3023, 7, 25)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.shouldHaveValue("07/25/3023")
            }
            reportRendering("date-picker.us-locale-format")
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
                    datePaid = LocalDate.of(3024, 1, 15)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.openPopover()
            }
            reportRendering("date-picker.popover-open")

            datePaid {
                input.clickDay(20)
                input.shouldHaveValue("20/01/3024")
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Date should be stored as 2024-01-20") {
                datePaid.shouldBe(LocalDate.of(3024, 1, 20))
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
                    datePaid = LocalDate.of(3023, 7, 25)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.shouldHaveValue("25/07/3023")
            }
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
                    datePaid = LocalDate.of(3020, 1, 1)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.fill("31/12/3023")
                input.shouldHaveValue("31/12/3023")
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Date should be stored as 2023-12-31") {
                datePaid.shouldBe(LocalDate.of(3023, 12, 31))
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
                    datePaid = LocalDate.of(3020, 1, 1)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.fill("29/02/3024")
                input.shouldHaveValue("29/02/3024")
            }

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Date should be stored as 2024-02-29") {
                datePaid.shouldBe(LocalDate.of(3024, 2, 29))
            }
    }

    @Test
    fun `should display empty input after clearing value`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    datePaid = LocalDate.of(3023, 7, 25)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.shouldHaveValue("25/07/3023")
                input.clear()
                input.shouldHaveValue("")
            }
            reportRendering("date-picker.empty-input")
        }
    }

    @Test
    fun `should display translated popover for English language`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    i18nSettings = I18nSettings(locale = "en_US", language = "en")
                )
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    datePaid = LocalDate.of(3024, 1, 15)
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            datePaid {
                input.openPopover()
                input.shouldHavePopoverMonthYear("3024 January")
                input.shouldHavePopoverWeekday("Mo")
                input.shouldHavePopoverWeekday("Tu")
            }
            reportRendering("date-picker.popover-english")
        }
    }

    @Test
    fun `should display translated popover for Ukrainian language`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    i18nSettings = I18nSettings(locale = "uk_UA", language = "uk")
                )
                val workspace = workspace(owner = fry, defaultCurrency = "UAH")
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    datePaid = LocalDate.of(3024, 1, 15)
                )
            }
        }

        // Note: Using direct locator access here due to Ukrainian language - page object validation expects English
        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.assumeEditExpensePage {
            datePaidUk {
                input.openPopover()
                input.shouldHavePopoverMonthYear("3024 Січень")
                input.shouldHavePopoverWeekday("пн")
                input.shouldHavePopoverWeekday("вт")
            }
            reportRendering("date-picker.popover-ukrainian")
        }
    }

    @Test
    fun `should preserve selected date when saving an expense`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "AUD")
                val category = category(workspace = workspace)
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/create")

        page.shouldBeCreateExpensePage {
            datePaid {
                input.fill("31/12/3023")
                input.shouldHaveValue("31/12/3023")
            }

            title.input.fill("Intergalactic timezone expense")
            category.input.selectOption(preconditions.category.name)
            originalAmount.input.fill("1000")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        val savedExpense = aggregateTemplate.findAll<Expense>()
            .first { it.title == "Intergalactic timezone expense" }

        savedExpense.datePaid.shouldBe(LocalDate.of(3023, 12, 31))
    }
}
