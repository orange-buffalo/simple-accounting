package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import org.junit.jupiter.api.Test

class SaCategorySelectFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display searchable category options with workspace isolation`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry).also {
                    category(workspace = it, name = "Delivery")
                    category(workspace = it, name = "Robot maintenance")
                }
                val expense = expense(workspace = workspace)
            }
        }
        preconditions {
            val amy = platformUser(userName = "amy")
            category(workspace = workspace(owner = amy), name = "Robot maintenance from another workspace")
        }

        page.authenticateViaCookie(testData.fry)
        page.openExpenseForCategorySelection(testData.expense.id!!) {
            category {
                input.search("Robot")
                input.shouldHaveOptions { options ->
                    options.shouldContainExactly("Robot maintenance")
                }
            }
        }
    }

    @Test
    fun `should load selected category and save selected category id`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val newCategory = category(workspace = workspace, name = "Robot maintenance")
                val expense = expense(
                    workspace = workspace,
                    category = category(workspace = workspace, name = "Delivery")
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openExpenseForCategorySelection(testData.expense.id!!) {
            category {
                input.shouldHaveSelectedValue("Delivery")
                input.selectOption("Robot maintenance")
            }
            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .should {
                it.categoryId.shouldBe(testData.newCategory.id)
            }
    }

    private fun Page.openExpenseForCategorySelection(
        expenseId: String,
        spec: EditExpensePage.() -> Unit
    ) {
        navigate("/expenses/$expenseId/edit")
        shouldBeEditExpensePage(spec)
    }
}
