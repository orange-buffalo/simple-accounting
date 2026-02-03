package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import org.junit.jupiter.api.Test

/**
 * Comprehensive full stack tests for Notes Input component (SaNotesInput).
 * Uses Edit Expense page with Notes input as testing grounds.
 */
class NotesInputFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should accept plain text input`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val expense = expense(workspace = workspace(owner = fry))
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
                val expense = expense(workspace = workspace(owner = fry))
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                input.fill("# Important Heading")
                input.shouldHaveValue("# Important Heading")
                // Advance clock past debounce timeout (300ms)
                page.clock().runFor(400)
                input.shouldHavePreviewWithHeading("Important Heading")
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
    fun `should show no preview when input is empty`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val expense = expense(workspace = workspace(owner = fry))
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
                val expense = expense(
                    workspace = workspace(owner = fry),
                    notes = """
                        # Meeting Notes
                        
                        ## Discussion Points
                        - Budget review
                        - **Urgent**: Approval needed
                        
                        See [details](https://example.com)
                    """.trimIndent()
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                // Advance clock past debounce timeout for pre-filled content
                page.clock().runFor(400)
                input.shouldHavePreviewWithHeading("Meeting Notes")
            }
            reportRendering("notes-input.pre-filled-edit-mode")
        }
    }

    @Test
    fun `should update preview when input changes`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val expense = expense(workspace = workspace(owner = fry))
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                input.fill("Initial text")
                page.clock().runFor(400)
                input.shouldHavePreview()
            }
            reportRendering("notes-input.preview-update-step-1")

            notes {
                input.fill("**Updated** text")
                page.clock().runFor(400)
                input.shouldHavePreview()
            }
            reportRendering("notes-input.preview-update-step-2")
        }
    }

    @Test
    fun `should render complex markdown in preview`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val expense = expense(workspace = workspace(owner = fry))
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            notes {
                input.fill("# Heading with **bold** and *italic* text")
                // Advance clock past debounce timeout
                page.clock().runFor(400)
                input.shouldHavePreviewWithHeading("Heading with")
            }
            reportRendering("notes-input.complex-markdown")
        }
    }
}
