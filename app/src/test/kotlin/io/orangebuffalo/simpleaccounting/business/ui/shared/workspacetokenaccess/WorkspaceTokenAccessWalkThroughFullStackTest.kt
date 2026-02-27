package io.orangebuffalo.simpleaccounting.business.ui.shared.workspacetokenaccess

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.IncomesOverviewPage.Companion.shouldBeIncomesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.incometaxpayments.IncomeTaxPaymentsOverviewPage.Companion.shouldBeIncomeTaxPaymentsOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.InvoicesOverviewPage.Companion.shouldBeInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.reporting.ReportingPage.Companion.shouldBeReportingPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.shouldBeLoginPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.NavigationMenu.MenuItem
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class WorkspaceTokenAccessWalkThroughFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should walk through all pages with write access token`(page: Page) {
        page.navigate("/login-by-link/${preconditions.token}")
        page.clock().runFor(1000)
        page.shouldBeDashboardPage()

        // Verify the menu structure - transient users should not see Settings or My Profile
        page.shouldHaveSideMenu().apply {
            shouldHaveWorkspaceName("Planet Express")
            shouldHaveItems(
                MenuItem("Dashboard", false),
                MenuItem("Expenses", false),
                MenuItem("Incomes", false),
                MenuItem("Invoices", false),
                MenuItem("Income Tax Payments", false),
                MenuItem("Reporting", false),
                MenuItem("USER", true),
                MenuItem("Logout", false),
            )
        }

        // Dashboard: verify expenses card shows correct data
        page.shouldBeDashboardPage {
            shouldBeLoaded()
            expensesCard {
                shouldBeLoaded()
                shouldHaveAmount("$100.00")
                shouldHaveFinalizedText("Total of 1 expenses")
            }
        }

        // Expenses overview: verify items and disabled create button
        page.shouldHaveSideMenu().clickExpenses()
        page.shouldBeExpensesOverviewPage {
            pageItems.shouldHaveTitles("Slurm supplies")
            createButton.shouldBeDisabled()
        }

        // Incomes overview: verify items and disabled create button
        page.shouldHaveSideMenu().clickIncomes()
        page.shouldBeIncomesOverviewPage {
            pageItems.shouldHaveTitles("Delivery to Mars")
            createButton.shouldBeDisabled()
        }

        // Invoices overview: verify items and disabled create button
        page.shouldHaveSideMenu().clickInvoices()
        page.shouldBeInvoicesOverviewPage {
            pageItems.shouldHaveTitles("Dark matter shipment")
            createButton.shouldBeDisabled()
        }

        // Income Tax Payments overview: verify items and disabled create button
        page.shouldHaveSideMenu().clickIncomeTaxPayments()
        page.shouldBeIncomeTaxPaymentsOverviewPage {
            pageItems.shouldHaveTitles("Tax for 3024")
            createButton.shouldBeDisabled()
        }

        // Reporting page: verify it opens
        page.shouldHaveSideMenu().clickReporting()
        page.shouldBeReportingPage()

        // Navigate back to Dashboard via menu to verify navigation works
        page.shouldHaveSideMenu().clickDashboard()
        page.shouldBeDashboardPage()

        // Logout and verify navigation to login page
        page.shouldHaveSideMenu().clickLogout()
        page.shouldBeLoginPage {}
    }

    private val preconditions by lazyPreconditions {
        object {
            val token = fry().let { fry ->
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
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                )
                income(
                    workspace = workspace,
                    category = category,
                    title = "Delivery to Mars",
                    dateReceived = LocalDate.of(1999, 2, 15),
                    currency = "USD",
                    originalAmount = 20000,
                    convertedAmounts = amountsInDefaultCurrency(20000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(20000),
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
                ).token
            }
        }
    }
}
