package io.orangebuffalo.simpleaccounting.business.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.shouldBeLoginPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.business.ui.user.categories.CategoriesOverviewPage.Companion.shouldBeCategoriesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.customers.CustomersOverviewPage.Companion.shouldBeCustomersOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.GeneralTaxesOverviewPage.Companion.shouldBeGeneralTaxesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.IncomesOverviewPage.Companion.shouldBeIncomesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.incometaxpayments.IncomeTaxPaymentsOverviewPage.Companion.shouldBeIncomeTaxPaymentsOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.InvoicesOverviewPage.Companion.shouldBeInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.reporting.ReportingPage.Companion.shouldBeReportingPage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspacesOverviewPage.Companion.shouldBeWorkspacesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.NavigationMenu.MenuItem
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import org.junit.jupiter.api.Test

class UserNavigationMenuFullStackTest : SaFullStackTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().also {
                workspace(owner = it, name = "Planet Express")
            }
        }
    }

    @Test
    fun `should render proper menu items and navigate to all pages for regular user`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/")
        page.shouldBeDashboardPage()

        page.shouldHaveSideMenu().apply {
            shouldHaveWorkspaceName("Planet Express")
            shouldHaveItems(
                MenuItem("Dashboard", false),
                MenuItem("Expenses", false),
                MenuItem("Incomes", false),
                MenuItem("Invoices", false),
                MenuItem("Income Tax Payments", false),
                MenuItem("Reporting", false),
                MenuItem("SETTINGS", true),
                MenuItem("Customers", false),
                MenuItem("Categories", false),
                MenuItem("General Taxes", false),
                MenuItem("Workspaces", false),
                MenuItem("USER", true),
                MenuItem("My Profile", false),
                MenuItem("Logout", false),
            )
        }

        page.shouldHaveSideMenu().clickExpenses()
        page.shouldBeExpensesOverviewPage()

        page.shouldHaveSideMenu().clickIncomes()
        page.shouldBeIncomesOverviewPage()

        page.shouldHaveSideMenu().clickInvoices()
        page.shouldBeInvoicesOverviewPage()

        page.shouldHaveSideMenu().clickIncomeTaxPayments()
        page.shouldBeIncomeTaxPaymentsOverviewPage()

        page.shouldHaveSideMenu().clickReporting()
        page.shouldBeReportingPage()

        page.shouldHaveSideMenu().clickCustomers()
        page.shouldBeCustomersOverviewPage()

        page.shouldHaveSideMenu().clickCategories()
        page.shouldBeCategoriesOverviewPage()

        page.shouldHaveSideMenu().clickGeneralTaxes()
        page.shouldBeGeneralTaxesOverviewPage()

        page.shouldHaveSideMenu().clickWorkspaces()
        page.shouldBeWorkspacesOverviewPage()

        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage()

        page.shouldHaveSideMenu().clickDashboard()
        page.shouldBeDashboardPage()

        page.shouldHaveSideMenu().clickLogout()
        page.shouldBeLoginPage {}
    }
}
