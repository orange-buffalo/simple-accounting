package io.orangebuffalo.simpleaccounting.web.ui.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class NavigationMenu(private val page: Page) {
    private val container = page.locator(".side-menu")
    fun shouldBeVisible(): NavigationMenu {
        container.assertThat().isVisible()
        return this
    }

    fun clickMyProfile(): NavigationMenu {
        page.getByText("My Profile").click()
        return this
    }
}

fun Page.shouldHaveSideMenu(): NavigationMenu = NavigationMenu(this).shouldBeVisible()
