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

class SaGeneralTaxSelectFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display searchable general tax options with workspace isolation`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry).also {
                    generalTax(workspace = it, title = "VAT")
                    generalTax(workspace = it, title = "GST")
                }
                val expense = expense(workspace = workspace)
            }
        }
        preconditions {
            val amy = platformUser(userName = "amy")
            generalTax(workspace = workspace(owner = amy), title = "GST from another workspace")
        }

        page.authenticateViaCookie(testData.fry)
        page.openExpenseForGeneralTaxSelection(testData.expense.id!!) {
            generalTax {
                input.search("GST")
                input.shouldHaveOptions { options ->
                    options.shouldContainExactly("GST")
                }
            }
        }
    }

    @Test
    fun `should select general tax and save selected general tax id`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val newGeneralTax = generalTax(workspace = workspace, title = "GST")
                val expense = expense(workspace = workspace)
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openExpenseForGeneralTaxSelection(testData.expense.id!!) {
            generalTax {
                input.selectOption("GST")
            }
            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .should {
                it.generalTaxId.shouldBe(testData.newGeneralTax.id)
            }
    }

    @Test
    fun `should load selected general tax and clear it`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val vat = generalTax(workspace = workspace, title = "VAT")
                val expense = expense(
                    workspace = workspace,
                    category = category(workspace = workspace, name = "Delivery"),
                    generalTax = vat,
                    title = "Robot oil"
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openExpenseForGeneralTaxSelection(testData.expense.id!!) {
            generalTax {
                input.shouldHaveSelectedValue("VAT")
                input.shouldHaveClearButton()
                input.clearSelection()
                input.shouldBeEmpty()
            }
            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .should {
                it.generalTaxId.shouldBe(null)
            }
    }

    private fun Page.openExpenseForGeneralTaxSelection(
        expenseId: String,
        spec: EditExpensePage.() -> Unit
    ) {
        navigate("/expenses/$expenseId/edit")
        shouldBeEditExpensePage(spec)
    }
}
