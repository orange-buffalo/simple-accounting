package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateExpensePage.Companion.openCreateExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateExpensePage.Companion.shouldBeCreateExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditExpensePage.Companion.shouldBeEditExpensePage
import org.junit.jupiter.api.Test

/**
 * Comprehensive full stack tests for Select component (SaFormSelect and SaCategoryInput).
 * Uses Create Expense and Edit Expense pages with Category input as testing grounds.
 */
class SelectFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should load with empty selection and placeholder`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category {
                input.shouldBeEmpty()
                input.shouldHavePlaceholder("Select a category")
            }
            reportRendering("select.empty-with-placeholder")
        }
    }

    @Test
    fun `should display categories when dropdown is opened`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    category(workspace = workspace, name = "Office Supplies")
                    category(workspace = workspace, name = "Travel")
                    category(workspace = workspace, name = "Marketing")
                }
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category {
                input.shouldHaveOptions { options ->
                    options.shouldWithClue("Should display all created categories") {
                        shouldContainExactlyInAnyOrder("Office Supplies", "Travel", "Marketing")
                    }
                    this@openCreateExpensePage.reportRendering("select.open-with-items")
                }
            }
        }
    }

    @Test
    fun `should display empty dropdown when no categories exist`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category {
                input.shouldHaveOptions { options ->
                    options.shouldWithClue("Should have no categories") {
                        shouldBeEmpty()
                    }
                    this@openCreateExpensePage.reportRendering("select.open-empty")
                }
            }
        }
    }

    @Test
    fun `should select a category from dropdown`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    category(workspace = workspace, name = "Office Supplies")
                    category(workspace = workspace, name = "Travel")
                }
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category {
                input.shouldBeEmpty()
                input.selectOption("Travel")
                input.shouldHaveSelectedValue("Travel")
            }
            reportRendering("select.with-selection")
        }
    }

    @Test
    fun `should load with pre-selected category in edit mode`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry).also {
                    category(workspace = it, name = "Travel")
                }
                val officeCategory = category(workspace = workspace, name = "Office Supplies")
                val expense = expense(
                    workspace = workspace,
                    category = officeCategory,
                    title = "Test Expense"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            category {
                input.shouldHaveSelectedValue("Office Supplies")
            }
        }
    }

    @Test
    fun `should change selected category`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry).also {
                    category(workspace = it, name = "Travel")
                }
                val officeCategory = category(workspace = workspace, name = "Office Supplies")
                val expense = expense(
                    workspace = workspace,
                    category = officeCategory,
                    title = "Test Expense"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            category {
                input.shouldHaveSelectedValue("Office Supplies")
                input.shouldHaveOptions {
                    this@shouldBeEditExpensePage.reportRendering("select.open-with-selection")
                }
                input.selectOption("Travel")
                input.shouldHaveSelectedValue("Travel")
            }
        }
    }

    @Test
    fun `should display loading state while fetching categories`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.authenticateViaCookie(preconditions.fry)

        // Block the categories API and verify loading state
        page.withBlockedApiResponse(
            "**/categories*",
            initiator = {
                page.navigate("/expenses/create")
            },
            blockedRequestSpec = {
                page.shouldBeCreateExpensePage {
                    category {
                        shouldBeLoading()
                    }
                    reportRendering("select.loading")
                }
            }
        )
    }

    @Test
    fun `should display single category in dropdown`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    category(workspace = workspace, name = "Only Category")
                }
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category {
                input.shouldHaveOptions { options ->
                    options.shouldWithClue("Should have exactly one category") {
                        shouldContainExactlyInAnyOrder("Only Category")
                    }
                }
            }
        }
    }

    @Test
    fun `should display many categories in dropdown`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    (1..10).forEach { i ->
                        category(workspace = workspace, name = "Category $i")
                    }
                }
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            category {
                input.shouldHaveOptions { options ->
                    options.shouldWithClue("Should have 10 categories") {
                        shouldContainExactly(
                            "Category 1", "Category 10", "Category 2", "Category 3", "Category 4",
                            "Category 5", "Category 6", "Category 7", "Category 8", "Category 9"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `should clear selected value when clearable is enabled`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val vat = generalTax(workspace = workspace, title = "VAT")
                val gst = generalTax(workspace = workspace, title = "GST")
                val expense = expense(
                    workspace = workspace,
                    category = category(workspace = workspace, name = "Office"),
                    generalTax = vat,
                    title = "Test Expense"
                )
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.expense.id}/edit")
        page.shouldBeEditExpensePage {
            generalTax {
                input.shouldHaveSelectedValue("VAT")
                input.shouldHaveClearButton()
                input.clearSelection()
                input.shouldBeEmpty()
            }
        }
    }
}
