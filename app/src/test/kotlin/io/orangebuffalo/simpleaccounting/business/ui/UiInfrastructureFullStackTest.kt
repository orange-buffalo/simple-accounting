package io.orangebuffalo.simpleaccounting.business.ui

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshToken
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.openLoginPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.shouldBeLoginPage
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.deleteAll

class UiInfrastructureFullStackTest(
    @param:Autowired private val jwtService: JwtService,
) : SaFullStackTestBase() {

    @Test
    fun `should redirect to login page when auth expired`(page: Page) {
        // Note: mockCurrentTime is already called by SaFullStackTestBase.setupFullStackTest()
        // No additional time mocking needed here

        // ensure any business page is loaded
        page.openLoginPage {
            loginInput { fill(preconditions.fry.userName) }
            passwordInput { fill(preconditions.fry.passwordHash) }
            loginButton { click() }
        }
        page.shouldBeDashboardPage { shouldBeLoaded() }

        // force auth expiration
        jwtService.rotateKeys()
        aggregateTemplate.deleteAll<RefreshToken>()

        // navigate to a business page loading data via API
        page.shouldHaveSideMenu().clickMyProfile()

        page.shouldBeLoginPage {
            reportRendering("ui-infra.expired-auth.login-page")
            shouldHaveNotifications {
                warning("Your session has expired. Please login again.")
            }
            loginInput { fill(preconditions.fry.userName) }
            passwordInput { fill(preconditions.fry.passwordHash) }
            loginButton { click() }
        }

        // TODO #2066: should be my profile page
        page.shouldBeDashboardPage {}
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
        }
    }
}
