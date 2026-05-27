package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.EditInvoicePage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.EditInvoicePage.Companion.shouldBeEditInvoicePage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.InvoicesOverviewPage.Companion.shouldBeInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import org.junit.jupiter.api.Test

class SaCustomerSelectFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display searchable customer options with workspace isolation`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry).also {
                    customer(workspace = it, name = "Mom's Friendly Robot Company")
                    customer(workspace = it, name = "Slurm Corp")
                }
                val invoice = invoice(customer = customer(workspace = workspace, name = "Planet Express Customer"))
            }
        }
        preconditions {
            val amy = platformUser(userName = "amy")
            customer(workspace = workspace(owner = amy), name = "Slurm Corp from another workspace")
        }

        page.authenticateViaCookie(testData.fry)
        page.openInvoiceForCustomerSelection(testData.invoice.id!!) {
            customer {
                input.search("Slurm")
                input.shouldHaveOptions { options ->
                    options.shouldContainExactly("Slurm Corp")
                }
            }
        }
    }

    @Test
    fun `should load selected customer and save selected customer id`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val newCustomer = customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                val invoice = invoice(customer = customer(workspace = workspace, name = "Planet Express Customer"))
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openInvoiceForCustomerSelection(testData.invoice.id!!) {
            customer {
                input.shouldHaveSelectedValue("Planet Express Customer")
                input.selectOption("Mom's Friendly Robot Company")
            }
            saveButton.click()
        }

        page.shouldBeInvoicesOverviewPage()

        aggregateTemplate.findSingle<Invoice>(testData.invoice.id!!)
            .should {
                it.customerId.shouldBe(testData.newCustomer.id)
            }
    }

    private fun Page.openInvoiceForCustomerSelection(
        invoiceId: String,
        spec: EditInvoicePage.() -> Unit
    ) {
        navigate("/invoices/$invoiceId/edit")
        shouldBeEditInvoicePage(spec)
    }
}
