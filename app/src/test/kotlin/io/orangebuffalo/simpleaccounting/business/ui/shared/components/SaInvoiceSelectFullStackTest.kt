package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.EditIncomePage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.EditIncomePage.Companion.shouldBeEditIncomePage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.IncomesOverviewPage.Companion.shouldBeIncomesOverviewPage
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.InvoiceOption
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaInvoiceSelectFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display searchable invoice options with rich labels and workspace isolation`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD").also {
                    val momCorp = customer(workspace = it, name = "Mom's Friendly Robot Company")
                    val slurmCorp = customer(workspace = it, name = "Slurm Corp")
                    invoice(
                        customer = momCorp,
                        title = "Delivery to Mars",
                        amount = 15000,
                        dateIssued = LocalDate.of(3025, 1, 10),
                        status = InvoiceStatus.SENT
                    )
                    invoice(
                        customer = slurmCorp,
                        title = "Slurm supplies",
                        amount = 25000,
                        dateIssued = LocalDate.of(3025, 1, 12),
                        status = InvoiceStatus.SENT
                    )
                }
                val income = income(workspace = workspace)
            }
        }
        preconditions {
            val amy = platformUser(userName = "amy")
            val otherWorkspace = workspace(owner = amy, defaultCurrency = "USD")
            val customer = customer(workspace = otherWorkspace)
            invoice(
                customer = customer,
                title = "Slurm supplies from another workspace",
                amount = 99000,
                dateIssued = LocalDate.of(3025, 1, 13),
                status = InvoiceStatus.SENT
            )
        }

        page.openIncomeForInvoiceSelection(testData.fry, testData.income.id!!) {
            linkedInvoice {
                input.search("Slurm")
                input.shouldHaveInvoiceOptions { options ->
                    options.shouldContainExactly(
                        InvoiceOption(
                            title = "Slurm supplies",
                            date = "12 Jan 3025",
                            amount = "USD\u00a0250.00"
                        )
                    )
                }
            }
        }
    }

    @Test
    fun `should select invoice and save linked invoice id`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val customer = customer(workspace = workspace, name = "Mom's Friendly Robot Company").also {
                    invoice(
                        customer = it,
                        title = "Delivery to Omicron Persei 8",
                        amount = 25000,
                        dateIssued = LocalDate.of(3025, 1, 12),
                        status = InvoiceStatus.SENT
                    )
                }
                val invoice: Invoice = invoice(
                    customer = customer,
                    title = "Delivery to Mars",
                    amount = 15000,
                    dateIssued = LocalDate.of(3025, 1, 10),
                    status = InvoiceStatus.SENT
                )
                val income = income(workspace = workspace)
            }
        }

        page.openIncomeForInvoiceSelection(testData.fry, testData.income.id!!) {
            linkedInvoice {
                input.selectOption("Delivery to Mars")
            }

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>(testData.income.id!!)
            .should {
                it.linkedInvoiceId.shouldBe(testData.invoice.id)
            }
    }

    @Test
    fun `should load selected invoice and clear it`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val invoice = invoice(
                    customer = customer(workspace = workspace, name = "Mom's Friendly Robot Company"),
                    title = "Delivery to Mars",
                    amount = 15000,
                    dateIssued = LocalDate.of(3025, 1, 10),
                    status = InvoiceStatus.SENT
                )
                val income = income(
                    workspace = workspace,
                    linkedInvoice = invoice
                )
            }
        }

        page.openIncomeForInvoiceSelection(testData.fry, testData.income.id!!) {
            linkedInvoice {
                input.shouldHaveSelectedValue("Delivery to Mars")
                input.shouldHaveClearButton()
                input.clearSelection()
                input.shouldBeEmpty()
            }

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>(testData.income.id!!)
            .should {
                it.linkedInvoiceId.shouldBe(null)
            }
    }

    @Test
    fun `should show more elements indicator when invoice result limit is exceeded`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD").also {
                    val customer = customer(workspace = it, name = "Mom's Friendly Robot Company")
                    (1..15).forEach { idx ->
                        invoice(
                            customer = customer,
                            title = "Planet Express invoice $idx",
                            amount = (10000 + idx * 1000).toLong(),
                            dateIssued = LocalDate.of(3025, 1, idx),
                            status = InvoiceStatus.SENT
                        )
                    }
                }
                val income = income(workspace = workspace)
            }
        }

        page.openIncomeForInvoiceSelection(testData.fry, testData.income.id!!) {
            linkedInvoice {
                input.search("")
                input.shouldShowMoreElementsIndicator(5)
            }
        }
    }

    @Test
    fun `should show no data message when no invoice matches search`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD").also {
                    invoice(
                        customer = customer(workspace = it, name = "Mom's Friendly Robot Company"),
                        title = "Delivery to Mars",
                        amount = 15000,
                        dateIssued = LocalDate.of(3025, 1, 10),
                        status = InvoiceStatus.SENT
                    )
                }
                val income = income(workspace = workspace)
            }
        }

        page.openIncomeForInvoiceSelection(testData.fry, testData.income.id!!) {
            linkedInvoice {
                input.search("Robot devil opera")
                input.shouldHaveNoDataMessage()
            }
        }
    }

    private fun Page.openIncomeForInvoiceSelection(
        user: PlatformUser,
        incomeId: String,
        spec: EditIncomePage.() -> Unit
    ) {
        authenticateViaCookie(user)
        navigate("/incomes/$incomeId/edit")
        shouldBeEditIncomePage(spec)
    }
}
