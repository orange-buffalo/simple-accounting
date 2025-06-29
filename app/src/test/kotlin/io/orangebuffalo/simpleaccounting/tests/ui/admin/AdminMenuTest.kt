package io.orangebuffalo.simpleaccounting.tests.ui.admin

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.ui.shared.components.NavigationMenu
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.tests.ui.shared.components.shouldHaveSideMenu
import org.junit.jupiter.api.Test

class AdminMenuTest : SaFullStackTestBase() {
    private val preconditions by lazyPreconditions {
        object {
            val admin = farnsworth()
        }
    }

    @Test
    fun `should render proper menu to admin user`(page: Page) {
        page.loginAs(preconditions.admin)
        page.shouldHaveSideMenu()
            .shouldHaveItems(
                NavigationMenu.MenuItem("Users", false),
                NavigationMenu.MenuItem("USER", true),
                NavigationMenu.MenuItem("My Profile", false),
                NavigationMenu.MenuItem("Logout", false),
            )
    }
}
