package io.orangebuffalo.simpleaccounting.business.ui.shared.workspacetokenaccess

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPayment
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.CreateExpensePage.Companion.shouldBeCreateExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.CreateIncomePage.Companion.shouldBeCreateIncomePage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.CreateIncomeTaxPaymentPage.Companion.shouldBeCreateIncomeTaxPaymentPage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.EditIncomePage.Companion.shouldBeEditIncomePage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.EditIncomeTaxPaymentPage.Companion.shouldBeEditIncomeTaxPaymentPage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.IncomesOverviewPage.Companion.shouldBeIncomesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.incometaxpayments.IncomeTaxPaymentsOverviewPage.Companion.shouldBeIncomeTaxPaymentsOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.CreateInvoicePage.Companion.shouldBeCreateInvoicePage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.EditInvoicePage.Companion.shouldBeEditInvoicePage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.InvoicesOverviewPage.Companion.shouldBeInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.reporting.ReportingPage.Companion.shouldBeReportingPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.NavigationMenu.MenuItem
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaActionLink.Companion.copyActionLinkValue
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaActionLink.Companion.editActionLinkValue
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class WorkspaceTokenAccessWalkThroughFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should walk through all pages with write access token`(page: Page) {
        // Login by navigating to the token URL
        page.navigate("/login-by-link/${preconditions.accessToken.token}")
        page.shouldBeDashboardPage()

        // Verify the menu structure - transient users should not see Settings section
        page.shouldHaveSideMenu().apply {
            shouldHaveWorkspaceName("Planet Express")
            shouldHaveItems(
                MenuItem("Dashboard", false),
                MenuItem("Expenses", false),
                MenuItem("Incomes", false),
                MenuItem("Invoices", false),
                MenuItem("Income Tax Payments", false),
                MenuItem("Reporting", false),
            )
        }

        // Dashboard: verify expenses card shows correct data
        page.shouldBeDashboardPage {
            shouldBeLoaded()
            expensesCard {
                shouldBeLoaded()
                shouldHaveAmount("USD 100.00")
                shouldHaveFinalizedText("Total of 1 expense")
            }
        }

        // Expenses overview: verify items and actions
        page.shouldHaveSideMenu().clickExpenses()
        page.shouldBeExpensesOverviewPage {
            pageItems.shouldHaveTitles("Slurm supplies")
            pageItems.staticItems[0].shouldHaveDetails(
                actions = listOf(copyActionLinkValue(), editActionLinkValue()),
            )
        }

        // Edit existing expense: verify title loads, change it, save
        page.shouldBeExpensesOverviewPage {
            pageItems.staticItems[0].executeEditAction()
        }
        page.shouldBeEditExpensePage {
            title { input.shouldHaveValue("Slurm supplies") }
            title { input.fill("Robot oil supplies") }
            saveButton.click()
        }
        page.shouldBeExpensesOverviewPage {
            pageItems.shouldHaveTitles("Robot oil supplies")
        }

        // Create new expense
        page.shouldBeExpensesOverviewPage {
            createButton.click()
        }
        page.shouldBeCreateExpensePage {
            title { input.fill("Spaceship fuel") }
            datePaid { input.fill("3025-06-15") }
            originalAmount { input.fill("42.00") }
            saveButton.click()
        }
        page.shouldBeExpensesOverviewPage {
            pageItems.shouldHaveTitles("Robot oil supplies", "Spaceship fuel")
        }

        // Incomes overview: verify items and actions
        page.shouldHaveSideMenu().clickIncomes()
        page.shouldBeIncomesOverviewPage {
            pageItems.shouldHaveTitles("Delivery to Mars")
            pageItems.staticItems[0].shouldHaveDetails(
                actions = listOf(editActionLinkValue()),
            )
        }

        // Edit existing income: verify title loads, change it, save
        page.shouldBeIncomesOverviewPage {
            pageItems.staticItems[0].executeEditAction()
        }
        page.shouldBeEditIncomePage {
            title { input.shouldHaveValue("Delivery to Mars") }
            title { input.fill("Delivery to Omicron Persei 8") }
            saveButton.click()
        }
        page.shouldBeIncomesOverviewPage {
            pageItems.shouldHaveTitles("Delivery to Omicron Persei 8")
        }

        // Create new income
        page.shouldBeIncomesOverviewPage {
            createButton.click()
        }
        page.shouldBeCreateIncomePage {
            title { input.fill("Moon cargo delivery") }
            dateReceived { input.fill("3025-07-20") }
            originalAmount { input.fill("77.00") }
            saveButton.click()
        }
        page.shouldBeIncomesOverviewPage {
            pageItems.shouldHaveTitles("Delivery to Omicron Persei 8", "Moon cargo delivery")
        }

        // Invoices overview: verify items and actions
        page.shouldHaveSideMenu().clickInvoices()
        page.shouldBeInvoicesOverviewPage {
            pageItems.shouldHaveTitles("Dark matter shipment")
            pageItems.staticItems[0].shouldHaveDetails(
                actions = listOf(editActionLinkValue()),
            )
        }

        // Edit existing invoice: verify title loads, change it, save
        page.shouldBeInvoicesOverviewPage {
            pageItems.staticItems[0].executeEditAction()
        }
        page.shouldBeEditInvoicePage {
            title { input.shouldHaveValue("Dark matter shipment") }
            title { input.fill("Dark matter premium shipment") }
            saveButton.click()
        }
        page.shouldBeInvoicesOverviewPage {
            pageItems.shouldHaveTitles("Dark matter premium shipment")
        }

        // Create new invoice
        page.shouldBeInvoicesOverviewPage {
            createButton.click()
        }
        page.shouldBeCreateInvoicePage {
            customer { input.selectOption("Mom") }
            title { input.fill("Nibbler food delivery") }
            amount { input.fill("99.00") }
            dateIssued { input.fill("3025-08-01") }
            dueDate { input.fill("3025-09-01") }
            saveButton.click()
        }
        page.shouldBeInvoicesOverviewPage {
            pageItems.shouldHaveTitles("Dark matter premium shipment", "Nibbler food delivery")
        }

        // Income Tax Payments overview: verify items and actions
        page.shouldHaveSideMenu().clickIncomeTaxPayments()
        page.shouldBeIncomeTaxPaymentsOverviewPage {
            pageItems.shouldHaveTitles("Tax for 3024")
            pageItems.staticItems[0].shouldHaveDetails(
                actions = listOf(editActionLinkValue()),
            )
        }

        // Edit existing income tax payment: verify title loads, change it, save
        page.shouldBeIncomeTaxPaymentsOverviewPage {
            pageItems.staticItems[0].executeEditAction()
        }
        page.shouldBeEditIncomeTaxPaymentPage {
            title { input.shouldHaveValue("Tax for 3024") }
            title { input.fill("Tax for 3024 (amended)") }
            saveButton.click()
        }
        page.shouldBeIncomeTaxPaymentsOverviewPage {
            pageItems.shouldHaveTitles("Tax for 3024 (amended)")
        }

        // Create new income tax payment
        page.shouldBeIncomeTaxPaymentsOverviewPage {
            createButton.click()
        }
        page.shouldBeCreateIncomeTaxPaymentPage {
            title { input.fill("Tax for 3025") }
            datePaid { input.fill("3025-04-15") }
            reportingDate { input.fill("3025-12-31") }
            amount { input.fill("500.00") }
            saveButton.click()
        }
        page.shouldBeIncomeTaxPaymentsOverviewPage {
            pageItems.shouldHaveTitles("Tax for 3024 (amended)", "Tax for 3025")
        }

        // Reporting page: verify it opens
        page.shouldHaveSideMenu().clickReporting()
        page.shouldBeReportingPage()

        // Verify data was actually persisted for edits
        aggregateTemplate.findAll(Expense::class.java)
            .count { it.title == "Robot oil supplies" } shouldBe 1
        aggregateTemplate.findAll(Income::class.java)
            .count { it.title == "Delivery to Omicron Persei 8" } shouldBe 1
        aggregateTemplate.findAll(Invoice::class.java)
            .count { it.title == "Dark matter premium shipment" } shouldBe 1
        aggregateTemplate.findAll(IncomeTaxPayment::class.java)
            .count { it.title == "Tax for 3024 (amended)" } shouldBe 1

        // Verify data was actually persisted for creates
        aggregateTemplate.findAll(Expense::class.java)
            .count { it.title == "Spaceship fuel" } shouldBe 1
        aggregateTemplate.findAll(Income::class.java)
            .count { it.title == "Moon cargo delivery" } shouldBe 1
        aggregateTemplate.findAll(Invoice::class.java)
            .count { it.title == "Nibbler food delivery" } shouldBe 1
        aggregateTemplate.findAll(IncomeTaxPayment::class.java)
            .count { it.title == "Tax for 3025" } shouldBe 1
    }

    private val preconditions by lazyPreconditions {
        object {
            val accessToken = fry().let { fry ->
                val workspace = workspace(owner = fry, name = "Planet Express")
                val category = category(workspace = workspace, name = "Delivery")
                val customer = customer(workspace = workspace, name = "Mom")
                expense(
                    workspace = workspace,
                    category = category,
                    title = "Slurm supplies",
                    datePaid = LocalDate.of(1999, 2, 10),
                    currency = "USD",
                    originalAmount = 10000,
                )
                income(
                    workspace = workspace,
                    category = category,
                    title = "Delivery to Mars",
                    dateReceived = LocalDate.of(1999, 2, 15),
                    currency = "USD",
                    originalAmount = 20000,
                )
                invoice(
                    customer = customer,
                    title = "Dark matter shipment",
                    dateIssued = LocalDate.of(3025, 1, 1),
                    dueDate = LocalDate.of(3025, 2, 1),
                    dateSent = LocalDate.of(3025, 1, 2),
                    currency = "USD",
                    amount = 50000,
                    status = InvoiceStatus.SENT,
                )
                incomeTaxPayment(
                    workspace = workspace,
                    title = "Tax for 3024",
                    datePaid = LocalDate.of(3025, 3, 15),
                    reportingDate = LocalDate.of(3024, 12, 31),
                    amount = 30000,
                )
                workspaceAccessToken(
                    workspace = workspace,
                    token = "planet-express-write-token",
                    validTill = Instant.parse("9999-12-31T23:59:59Z"),
                    timeCreated = MOCK_TIME,
                )
            }
        }
    }
}
