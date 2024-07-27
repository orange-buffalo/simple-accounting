package io.orangebuffalo.simpleaccounting.tests.ui.shared.pages

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainInOrder
import io.orangebuffalo.simpleaccounting.tests.infra.utils.hasClass
import io.orangebuffalo.simpleaccounting.tests.infra.utils.innerTextOrNull
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeVisible

class NavigationMenu(private val page: Page) {
    private val container = page.locator(".side-menu")
    fun shouldBeVisible(): NavigationMenu {
        container.shouldBeVisible()
        return this
    }

    fun clickMyProfile(): NavigationMenu {
        container.getByText("My Profile").click()
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

    data class MenuItem(val label: String?, val isSectionHeader: Boolean)
}

fun Page.shouldHaveSideMenu(): NavigationMenu = NavigationMenu(this).shouldBeVisible()
