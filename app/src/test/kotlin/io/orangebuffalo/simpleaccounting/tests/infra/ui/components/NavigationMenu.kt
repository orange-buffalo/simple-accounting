package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainInOrder
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*

class NavigationMenu(private val page: Page) {
    private val container = page.locator(".side-menu")
    fun shouldBeVisible(): NavigationMenu {
        container.shouldBeVisible()
        return this
    }

    fun shouldBeHidden(): NavigationMenu {
        container.shouldBeHidden()
        return this
    }

    fun clickMyProfile(): NavigationMenu {
        container.getByText("My Profile").click()
        return this
    }

    fun clickDashboard(): NavigationMenu {
        container.getByText("Dashboard").click()
        return this
    }

    fun clickExpenses(): NavigationMenu {
        container.getByText("Expenses").click()
        return this
    }

    fun clickIncomes(): NavigationMenu {
        container.getByText("Incomes").click()
        return this
    }

    fun clickInvoices(): NavigationMenu {
        container.getByText("Invoices").click()
        return this
    }

    fun clickIncomeTaxPayments(): NavigationMenu {
        container.getByText("Income Tax Payments").click()
        return this
    }

    fun clickReporting(): NavigationMenu {
        container.getByText("Reporting").click()
        return this
    }

    fun clickWorkspaces(): NavigationMenu {
        container.getByText("Workspaces").click()
        return this
    }

    fun clickWorkspacesUk(): NavigationMenu {
        container.getByText("Проекти").click()
        return this
    }

    fun clickLogout(): NavigationMenu {
        container.getByText("Logout").click()
        return this
    }

    fun clickLogoutUk(): NavigationMenu {
        container.getByText("Вихід").click()
        return this
    }

    fun clickCustomers(): NavigationMenu {
        container.getByText("Customers").click()
        return this
    }

    fun clickCategories(): NavigationMenu {
        container.getByText("Categories").click()
        return this
    }

    fun clickGeneralTaxes(): NavigationMenu {
        container.getByText("General Taxes").click()
        return this
    }

    fun clickUsersOverview(): NavigationMenu {
        container.getByText("Users").click()
        return this
    }

    fun shouldHaveItems(vararg expectedItems: MenuItem) {
        val menuItems = page.locator(".side-menu__link, .side-menu__category")
            .elementHandles()
            .map { element ->
                val label = element.innerTextOrNull()
                val isSectionHeader = element.hasClass("side-menu__category")
                MenuItem(label, isSectionHeader)
            }
        menuItems.shouldContainInOrder(*expectedItems)
    }

    fun shouldHaveWorkspaceName(name: String) {
        container.locator(".side-menu__workspace-name").shouldHaveText(name)
    }

    data class MenuItem(val label: String?, val isSectionHeader: Boolean)
}

fun Page.shouldHaveSideMenu(): NavigationMenu = NavigationMenu(this).shouldBeVisible()

fun Page.shouldHaveSideMenuHidden() {
    NavigationMenu(this).shouldBeHidden()
}
