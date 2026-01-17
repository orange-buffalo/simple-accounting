package io.orangebuffalo.simpleaccounting.tests.ui

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshToken
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.ui.shared.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.LoginPage.Companion.openLoginPage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.LoginPage.Companion.shouldBeLoginPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.DashboardPage.Companion.shouldBeDashboardPage
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UiInfrastructureFullStackTest(
    @param:Autowired private val jwtService: JwtService,
) : SaFullStackTestBase() {

    @Test
    fun `should redirect to login page when auth expired`(page: Page) {
        // ensure any business page is loaded
        page.openLoginPage {
            loginInput { fill(preconditions.fry.userName) }
            passwordInput { fill(preconditions.fry.passwordHash) }
            loginButton { click() }
        }
        page.shouldBeDashboardPage { shouldBeLoaded() }

        // force auth expiration
        jwtService.rotateKeys()
        aggregateTemplate.deleteAll(RefreshToken::class.java)

        // Clear browser cookies to remove the refresh token
        page.context().clearCookies()
        
        // Try to navigate to a protected page - should redirect to login
        page.navigate("/app/#/dashboard")
        
        // should be redirected to login page (without the "session expired" message since we cleared cookies)
        page.shouldBeLoginPage {
            reportRendering("ui-infra.expired-auth.login-page")
            loginInput { fill(preconditions.fry.userName) }
            passwordInput { fill(preconditions.fry.passwordHash) }
            loginButton { click() }
        }

        // Should be able to login again
        page.shouldBeDashboardPage {}
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
        }
    }
}
