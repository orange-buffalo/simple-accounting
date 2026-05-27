package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.EditInvoicePage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.EditInvoicePage.Companion.shouldBeEditInvoicePage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.InvoicesOverviewPage.Companion.shouldBeInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import org.junit.jupiter.api.Test

class SaEntityInputSelectFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display searchable customer category and general tax options with workspace isolation`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry).also {
                    customer(workspace = it, name = "Mom's Friendly Robot Company")
                    customer(workspace = it, name = "Slurm Corp")
                    category(workspace = it, name = "Delivery")
                    category(workspace = it, name = "Robot maintenance")
                    generalTax(workspace = it, title = "VAT")
                    generalTax(workspace = it, title = "GST")
                }
                val invoice = invoice(customer = customer(workspace = workspace, name = "Planet Express Customer"))
                val expense = expense(workspace = workspace)
            }
        }
        preconditions {
            val amy = platformUser(userName = "amy")
            val otherWorkspace = workspace(owner = amy)
            customer(workspace = otherWorkspace, name = "Slurm Corp from another workspace")
            category(workspace = otherWorkspace, name = "Robot maintenance from another workspace")
            generalTax(workspace = otherWorkspace, title = "GST from another workspace")
        }

        page.authenticateViaCookie(testData.fry)
        page.openInvoiceForEntitySelection(testData.invoice.id!!) {
            customer {
                input.search("Slurm")
                input.shouldHaveOptions { options ->
                    options.shouldContainExactly("Slurm Corp")
                }
            }

            generalTax {
                input.search("GST")
                input.shouldHaveOptions { options ->
                    options.shouldContainExactly("GST")
                }
            }
        }

        page.openExpenseForEntitySelection(testData.expense.id!!) {
            category {
                input.search("Robot")
                input.shouldHaveOptions { options ->
                    options.shouldContainExactly("Robot maintenance")
                }
            }
        }
    }

    @Test
    fun `should select customer category and general tax and save selected ids`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val newCustomer = customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                val newCategory = category(workspace = workspace, name = "Robot maintenance")
                val newGeneralTax = generalTax(workspace = workspace, title = "GST")
                val invoice = invoice(customer = customer(workspace = workspace, name = "Planet Express Customer"))
                val expense = expense(
                    workspace = workspace,
                    category = category(workspace = workspace, name = "Delivery")
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openInvoiceForEntitySelection(testData.invoice.id!!) {
            customer {
                input.selectOption("Mom's Friendly Robot Company")
            }
            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
            .should {
                it.customerId.shouldBe(testData.newCustomer.id)
            }

        page.openExpenseForEntitySelection(testData.expense.id!!) {
            category {
                input.selectOption("Robot maintenance")
            }
            generalTax {
                input.selectOption("GST")
            }
            saveButton.click()
        }

        page.shouldBeExpensesOverviewPage()

        aggregateTemplate.findSingle<Expense>(testData.expense.id!!)
            .should {
                it.categoryId.shouldBe(testData.newCategory.id)
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
        page.openExpenseForEntitySelection(testData.expense.id!!) {
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

    private fun Page.openInvoiceForEntitySelection(
        invoiceId: String,
        spec: EditInvoicePage.() -> Unit
    ) {
        navigate("/invoices/$invoiceId/edit")
        shouldBeEditInvoicePage(spec)
    }

    private fun Page.openExpenseForEntitySelection(
        expenseId: String,
        spec: EditExpensePage.() -> Unit
    ) {
        navigate("/expenses/$expenseId/edit")
        shouldBeEditExpensePage(spec)
    }
}
