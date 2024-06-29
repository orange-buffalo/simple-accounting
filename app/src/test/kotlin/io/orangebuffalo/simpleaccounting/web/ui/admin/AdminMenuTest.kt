package io.orangebuffalo.simpleaccounting.web.ui.admin

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.NavigationMenu
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldHaveSideMenu
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class AdminMenuTest(
    preconditionsFactory: PreconditionsFactory,
) {
    private val preconditions by preconditionsFactory {
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
