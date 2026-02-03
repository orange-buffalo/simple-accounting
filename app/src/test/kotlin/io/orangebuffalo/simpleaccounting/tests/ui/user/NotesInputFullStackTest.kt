package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Comprehensive full stack tests for Notes Input component (SaNotesInput).
 * Uses Edit Expense page with Notes input as testing grounds.
 */
class NotesInputFullStackTest : SaFullStackTestBase() {

    @BeforeEach
    fun setup(page: Page) {
        // Resume clock to allow debounced markdown preview to work properly
        page.clock().resume()
    }

    @Test
    fun `should accept plain text input`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 100,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                input.fill("Simple plain text note")
                input.shouldHaveValue("Simple plain text note")
            }
            reportRendering("notes-input.plain-text")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Notes should be stored as plain text") {
                notes.shouldBe("Simple plain text note")
            }
    }

    @Test
    fun `should render markdown preview with headers`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 100,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                input.fill("# Important Heading")
                input.shouldHaveValue("# Important Heading")
                input.shouldHavePreview()
                input.preview().shouldContainHeading("Important Heading")
            }
            reportRendering("notes-input.markdown-heading")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Markdown should be stored as-is") {
                notes.shouldBe("# Important Heading")
            }
    }

    @Test
    fun `should render markdown preview with bold and italic`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 100,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                input.fill("**Bold text** and *italic text*")
                input.shouldHaveValue("**Bold text** and *italic text*")
                input.shouldHavePreview()
                input.preview().shouldContainBold("Bold text")
                input.preview().shouldContainItalic("italic text")
            }
            reportRendering("notes-input.markdown-bold-italic")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Markdown should be stored with formatting") {
                notes.shouldBe("**Bold text** and *italic text*")
            }
    }

    @Test
    fun `should render markdown preview with links`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 100,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                input.fill("Check [documentation](https://example.com)")
                input.shouldHaveValue("Check [documentation](https://example.com)")
                input.shouldHavePreview()
                input.preview().shouldContainLink("documentation")
            }
            reportRendering("notes-input.markdown-link")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Markdown links should be stored") {
                notes.shouldBe("Check [documentation](https://example.com)")
            }
    }

    @Test
    fun `should render markdown preview with lists`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 100,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                val listContent = """
                    - First item
                    - Second item
                    - Third item
                """.trimIndent()
                input.fill(listContent)
                input.shouldHaveValue(listContent)
                input.shouldHavePreview()
                input.preview().shouldContainListItem("First item")
                input.preview().shouldContainListItem("Second item")
                input.preview().shouldContainListItem("Third item")
            }
            reportRendering("notes-input.markdown-list")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Markdown list should be stored") {
                notes.shouldBe("- First item\n- Second item\n- Third item")
            }
    }

    @Test
    fun `should show no preview when input is empty`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 100,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                input.shouldNotHavePreview()
            }
            reportRendering("notes-input.empty-no-preview")
        }
    }

    @Test
    fun `should load pre-filled value in edit mode`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Existing Expense",
                    originalAmount = 100,
                    currency = "USD",
                    notes = "## Pre-filled notes\nWith some **important** content"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                input.shouldHaveValue("## Pre-filled notes\nWith some **important** content")
                input.shouldHavePreview()
                input.preview().shouldContainHeading("Pre-filled notes")
                input.preview().shouldContainBold("important")
            }
            reportRendering("notes-input.pre-filled-edit-mode")
        }
    }

    @Test
    fun `should update preview when input changes`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 100,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                input.fill("Initial text")
                input.shouldHavePreview()
                input.preview().shouldContainText("Initial text")
            }
            reportRendering("notes-input.preview-update-step-1")

            notes {
                input.fill("**Updated** text")
                input.shouldHavePreview()
                input.preview().shouldContainBold("Updated")
            }
            reportRendering("notes-input.preview-update-step-2")
        }
    }

    @Test
    fun `should handle multi-line markdown content`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val expense = expense(
                    workspace = workspace,
                    category = null,
                    title = "Test",
                    originalAmount = 100,
                    currency = "USD"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                val multiLineContent = """
                    # Meeting Notes
                    
                    ## Discussion Points
                    - Budget review
                    - **Urgent**: Approval needed
                    
                    See [details](https://example.com)
                """.trimIndent()
                input.fill(multiLineContent)
                input.shouldHaveValue(multiLineContent)
                input.shouldHavePreview()
                input.preview().shouldContainHeading("Meeting Notes")
                input.preview().shouldContainHeading("Discussion Points")
                input.preview().shouldContainListItem("Budget review")
                input.preview().shouldContainBold("Urgent")
                input.preview().shouldContainLink("details")
            }
            reportRendering("notes-input.multi-line-complex")

            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(preconditions.expense.id!!)
            .shouldWithClue("Multi-line markdown should be stored") {
                notes.shouldBe("# Meeting Notes\n\n## Discussion Points\n- Budget review\n- **Urgent**: Approval needed\n\nSee [details](https://example.com)")
            }
    }
}
