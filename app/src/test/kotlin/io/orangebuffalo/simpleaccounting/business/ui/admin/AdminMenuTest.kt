package io.orangebuffalo.simpleaccounting.business.ui.admin

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.admin.usermanagement.UsersOverviewPage.Companion.shouldBeUsersOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.loginAs
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.shouldBeLoginPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.shouldBeMyProfilePage
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
    fun `should render proper menu items and navigate to all pages for admin user`(page: Page) {
        page.loginAs(preconditions.admin)
        page.shouldBeUsersOverviewPage()

        page.shouldHaveSideMenu()
            .shouldHaveItems(
                NavigationMenu.MenuItem("Users", false),
                NavigationMenu.MenuItem("USER", true),
                NavigationMenu.MenuItem("My Profile", false),
                NavigationMenu.MenuItem("Logout", false),
            )

        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage()

        page.shouldHaveSideMenu().clickUsersOverview()
        page.shouldBeUsersOverviewPage()

        page.shouldHaveSideMenu().clickLogout()
        page.shouldBeLoginPage {}
    }
}
