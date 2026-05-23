package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeLessThan
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.EditIncomePage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.EditIncomePage.Companion.shouldBeEditIncomePage
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.InvoiceOption
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaEntitySelectFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should show more elements indicator when result limit is exceeded`(page: Page) {
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

        page.openIncomeForEntitySelection(testData.fry, testData.income.id!!) {
            linkedInvoice {
                input.search("")
                input.shouldShowMoreElementsIndicator(5)
            }
        }
    }

    @Test
    fun `should show no data message when no entity matches search`(page: Page) {
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

        page.openIncomeForEntitySelection(testData.fry, testData.income.id!!) {
            linkedInvoice {
                input.search("Robot devil opera")
                input.shouldHaveNoDataMessage()
            }
        }
    }

    @Test
    fun `should debounce remote search and use the final typed query`(page: Page) {
        val searchQuery = "Robot devil opera B"
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD").also {
                    // Titles differ only by the last character, so the expected single result proves
                    // that the final typed value, not an earlier prefix, was sent to the API.
                    invoice(
                        customer = customer(workspace = it, name = "Mom's Friendly Robot Company"),
                        title = "Robot devil opera A",
                        amount = 66500,
                        dateIssued = LocalDate.of(3025, 6, 5),
                        status = InvoiceStatus.SENT
                    )
                    invoice(
                        customer = customer(workspace = it, name = "Mom's Friendly Robot Company"),
                        title = searchQuery,
                        amount = 66600,
                        dateIssued = LocalDate.of(3025, 6, 6),
                        status = InvoiceStatus.SENT
                    )
                }
                val income = income(workspace = workspace)
            }
        }
        var invoiceSearchRequests = 0
        page.onRequest { request ->
            val postData = try { request.postData() } catch (e: Exception) { null }
            if (postData?.contains("\"operationName\":\"getInvoicesForSelect\"") == true) {
                invoiceSearchRequests += 1
            }
        }

        page.openIncomeForEntitySelection(testData.fry, testData.income.id!!) {
            linkedInvoice {
                input.typeSearch(searchQuery)
                input.shouldHaveInvoiceOptions { options ->
                    options.shouldContainExactly(
                        InvoiceOption(
                            title = searchQuery,
                            date = "6 Jun 3025",
                            amount = "USD\u00a0666.00"
                        )
                    )
                }
            }
        }
        // We intentionally do not assert an exact count: framework internals may legitimately trigger
        // one extra query, but debounce must still prevent one API request per typed character.
        invoiceSearchRequests.shouldBeLessThan(searchQuery.length)
    }

    private fun Page.openIncomeForEntitySelection(
        user: PlatformUser,
        incomeId: String,
        spec: EditIncomePage.() -> Unit
    ) {
        authenticateViaCookie(user)
        navigate("/incomes/$incomeId/edit")
        shouldBeEditIncomePage(spec)
    }
}
