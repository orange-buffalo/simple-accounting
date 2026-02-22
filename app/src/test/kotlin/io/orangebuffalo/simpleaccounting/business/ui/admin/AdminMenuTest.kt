package io.orangebuffalo.simpleaccounting.business.ui.admin

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.loginAs
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.NavigationMenu
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
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
