package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateIncomePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateIncomePage.Companion.openCreateIncomePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.IncomesOverviewPage.Companion.shouldBeIncomesOverviewPage
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Full stack tests for Create Income page, focusing on Entity Select component
 * testing with invoice selection functionality.
 */
class CreateIncomeFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should create income with basic fields`(page: Page) {
        page.setupPreconditionsAndNavigateToCreatePage {

            category { input.selectOption("Delivery") }
            title { input.fill("Payment for delivery services") }
            originalAmount { input.fill("100.00") }
            dateReceived { input.fill("3025-01-15") }

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>()
            .shouldBeEntityWithFields(
                Income(
                    title = "Payment for delivery services",
                    categoryId = preconditions.category.id!!,
                    dateReceived = LocalDate.of(3025, 1, 15),
                    currency = "USD",
                    originalAmount = 10000,
                    convertedAmounts = AmountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(10000),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = IncomeStatus.FINALIZED,
                    timeRecorded = MOCK_TIME,
                    workspaceId = preconditions.workspace.id!!,
                    generalTaxId = null,
                    linkedInvoiceId = null,
                ),
                ignoredProperties = arrayOf(
                    Income::id,
                    Income::version,
                )
            )
    }

    @Test
    fun `should select invoice from entity select dropdown`(page: Page) {
        val testPreconditions = preconditions {
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
            }
        }

        page.authenticateViaCookie(testPreconditions.fry)
        page.openCreateIncomePage {

            category { input.selectOption("Delivery") }
            title { input.fill("Payment for delivery services") }
            originalAmount { input.fill("150.00") }
            dateReceived { input.fill("3025-01-15") }

            // Select an invoice using EntitySelect
            linkedInvoice { input.selectOption("Delivery to Mars") }

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>()
            .should {
                it.linkedInvoiceId.shouldBe(testPreconditions.invoice1.id)
            }
    }

    @Test
    fun `should search for invoice in entity select`(page: Page) {
        val testPreconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val customer = customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                val invoice1 = invoice(
                    customer = customer,
                    title = "Delivery to Mars",
                    amount = 15000,
                    dateIssued = LocalDate.of(3025, 1, 10)
                )
                val invoice2 = invoice(
                    customer = customer,
                    title = "Interplanetary cargo shipment",
                    amount = 25000,
                    dateIssued = LocalDate.of(3025, 1, 12)
                )
                val invoice3 = invoice(
                    customer = customer,
                    title = "Robot parts delivery",
                    amount = 30000,
                    dateIssued = LocalDate.of(3025, 1, 13)
                )
            }
        }

        page.authenticateViaCookie(testPreconditions.fry)
        page.openCreateIncomePage {

            category { input.selectOption("Delivery") }
            title { input.fill("Payment received") }
            originalAmount { input.fill("250.00") }
            dateReceived { input.fill("3025-01-15") }

            // Test remote search functionality
            linkedInvoice {
                input.search("cargo")
                // Should only show the matching invoice
                input.shouldHaveOptions { options ->
                    options.shouldWithClue("Should only show matching invoice") {
                        size.shouldBe(1)
                        first().contains("Interplanetary cargo shipment").shouldBe(true)
                    }
                }
            }

            // Clear search and search for different term
            linkedInvoice {
                input.search("Robot")
                input.shouldHaveOptions { options ->
                    options.shouldWithClue("Should show Robot parts delivery") {
                        size.shouldBe(1)
                        first().contains("Robot parts delivery").shouldBe(true)
                    }
                }
            }

            // Select the found invoice
            linkedInvoice { input.selectOption("Robot parts delivery") }

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>()
            .should {
                it.linkedInvoiceId.shouldBe(testPreconditions.invoice3.id)
            }
    }

    @Test
    fun `should show more elements indicator when pagination limit is exceeded`(page: Page) {
        val testPreconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val customer = customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                // Create 15 invoices to exceed the default page size of 10
                val invoices = (1..15).map { index ->
                    invoice(
                        customer = customer,
                        title = "Invoice ${String.format("%02d", index)}",
                        amount = 10000L * index,
                        dateIssued = LocalDate.of(3025, 1, index)
                    )
                }
            }
        }

        page.authenticateViaCookie(testPreconditions.fry)
        page.openCreateIncomePage {

            category { input.selectOption("Delivery") }
            title { input.fill("Payment received") }
            originalAmount { input.fill("100.00") }
            dateReceived { input.fill("3025-01-20") }

            // Open dropdown and verify pagination indicator
            linkedInvoice {
                // Should show "5 more elements..." indicator
                input.shouldShowMoreElementsIndicator(5)
            }

            reportRendering("entity-select.more-elements-indicator")
        }
    }

    @Test
    fun `should clear selected invoice`(page: Page) {
        val testPreconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val customer = customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                val invoice = invoice(
                    customer = customer,
                    title = "Delivery services",
                    amount = 10000,
                    dateIssued = LocalDate.of(3025, 1, 10)
                )
            }
        }

        page.authenticateViaCookie(testPreconditions.fry)
        page.openCreateIncomePage {

            category { input.selectOption("Delivery") }
            title { input.fill("Payment for delivery") }
            originalAmount { input.fill("100.00") }
            dateReceived { input.fill("3025-01-15") }

            // Select an invoice
            linkedInvoice { input.selectOption("Delivery services") }
            linkedInvoice { input.shouldHaveSelectedValue("Delivery services") }

            // Clear the selection
            linkedInvoice {
                input.shouldHaveClearButton()
                input.clearSelection()
                input.shouldBeEmpty()
            }

            reportRendering("entity-select.cleared")

            saveButton.click()
        }

        page.shouldBeIncomesOverviewPage()

        aggregateTemplate.findSingle<Income>()
            .should {
                it.linkedInvoiceId.shouldBe(null)
            }
    }

    @Test
    fun `should display invoice details in entity select options`(page: Page) {
        val testPreconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val customer = customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                val invoice = invoice(
                    customer = customer,
                    title = "Spaceship fuel delivery",
                    amount = 50000,
                    currency = "USD",
                    dateIssued = LocalDate.of(3025, 1, 10)
                )
            }
        }

        page.authenticateViaCookie(testPreconditions.fry)
        page.openCreateIncomePage {

            category { input.selectOption("Delivery") }
            title { input.fill("Payment received") }
            originalAmount { input.fill("500.00") }
            dateReceived { input.fill("3025-01-15") }

            // Open dropdown to see invoice details

            reportRendering("entity-select.invoice-with-details")
        }
    }

    private fun Page.setupPreconditionsAndNavigateToCreatePage(spec: CreateIncomePage.() -> Unit) {
        authenticateViaCookie(preconditions.fry)
        openCreateIncomePage(spec)
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry, defaultCurrency = "USD")
            val category = category(workspace = workspace, name = "Delivery")
        }
    }
}
