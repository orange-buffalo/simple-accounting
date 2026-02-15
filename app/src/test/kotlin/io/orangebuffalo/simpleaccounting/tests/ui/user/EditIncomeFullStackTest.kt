package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditIncomePage.Companion.shouldBeEditIncomePage
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Full stack tests for Edit Income page, focusing on Entity Select component
 * testing with invoice selection functionality.
 */
class EditIncomeFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should select invoice from entity select dropdown`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val customer = customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                val invoice1 = invoice(
                    customer = customer,
                    title = "Delivery to Mars",
                    amount = 15000,
                    dateIssued = LocalDate.of(3025, 1, 10),
                    status = InvoiceStatus.SENT
                )
                val invoice2 = invoice(
                    customer = customer,
                    title = "Delivery to Omicron Persei 8",
                    amount = 25000,
                    dateIssued = LocalDate.of(3025, 1, 12),
                    status = InvoiceStatus.SENT
                )
                val income = income(
                    workspace = workspace,
                    category = category,
                    title = "Payment for delivery services",
                    originalAmount = 15000L,
                    dateReceived = LocalDate.of(3025, 1, 15)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/incomes/${testData.income.id}/edit")
        page.shouldBeEditIncomePage {
            // Select an invoice using EntitySelect
            linkedInvoice {
                input.selectOption("Delivery to Mars")
            }

            saveButton.click()
        }

        aggregateTemplate.findSingle<Income>(testData.income.id!!)
            .should {
                it.linkedInvoiceId.shouldBe(testData.invoice1.id)
            }
    }

    @Test
    fun `should search for invoice in entity select`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val customer1 = customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                val customer2 = customer(workspace = workspace, name = "Slurm Corp")
                val invoice1 = invoice(
                    customer = customer1,
                    title = "Delivery to Mars",
                    amount = 15000,
                    dateIssued = LocalDate.of(3025, 1, 10),
                    status = InvoiceStatus.SENT
                )
                val invoice2 = invoice(
                    customer = customer2,
                    title = "Slurm supplies",
                    amount = 25000,
                    dateIssued = LocalDate.of(3025, 1, 12),
                    status = InvoiceStatus.SENT
                )
                val income = income(
                    workspace = workspace,
                    category = category,
                    title = "Payment for delivery services",
                    originalAmount = 15000L,
                    dateReceived = LocalDate.of(3025, 1, 15)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/incomes/${testData.income.id}/edit")
        page.shouldBeEditIncomePage {
            linkedInvoice {
                // Search for "Slurm" should only match one invoice
                input.search("Slurm")
                input.shouldHaveInvoiceOptions { options ->
                    options.shouldHaveSize(1)
                    options[0].shouldBe(
                        InvoiceOption(
                            title = "Slurm supplies",
                            date = "12 Jan 3025",
                            amount = "$250.00"
                        )
                    )
                }
            }
        }
    }

    @Test
    fun `should show more elements indicator when pagination limit is exceeded`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val customer = customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                val invoices = (1..15).map { idx ->
                    invoice(
                        customer = customer,
                        title = "Invoice $idx",
                        amount = (10000 + idx * 1000).toLong(),
                        dateIssued = LocalDate.of(3025, 1, idx),
                        status = InvoiceStatus.SENT
                    )
                }
                val income = income(
                    workspace = workspace,
                    category = category,
                    title = "Payment",
                    originalAmount = 15000L,
                    dateReceived = LocalDate.of(3025, 1, 15)
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/incomes/${testData.income.id}/edit")
        page.shouldBeEditIncomePage {
            linkedInvoice {
                input.search("")
                input.shouldShowMoreElementsIndicator(5)
            }
        }
    }

    @Test
    fun `should clear selected invoice`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val customer = customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                val invoice = invoice(
                    customer = customer,
                    title = "Delivery to Mars",
                    amount = 15000,
                    dateIssued = LocalDate.of(3025, 1, 10),
                    status = InvoiceStatus.SENT
                )
                val income = income(
                    workspace = workspace,
                    category = category,
                    title = "Payment for delivery services",
                    originalAmount = 15000L,
                    dateReceived = LocalDate.of(3025, 1, 15),
                    linkedInvoice = invoice
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/incomes/${testData.income.id}/edit")
        page.shouldBeEditIncomePage {
            linkedInvoice {
                input.shouldHaveSelectedValue("Delivery to Mars")
                input.shouldHaveClearButton()
                input.clearSelection()
                input.shouldBeEmpty()
            }

            saveButton.click()
        }

        aggregateTemplate.findSingle<Income>(testData.income.id!!)
            .should {
                it.linkedInvoiceId.shouldBe(null)
            }
    }
}

/**
 * Represents an invoice option in the EntitySelect dropdown.
 * Contains the rich display content: title, date, and amount.
 */
data class InvoiceOption(
    val title: String,
    val date: String,
    val amount: String
)
