package io.orangebuffalo.simpleaccounting.tests.ui

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshToken
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TEST_FIXED_DATE_TIME
import io.orangebuffalo.simpleaccounting.tests.ui.shared.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.LoginPage.Companion.openLoginPage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.LoginPage.Companion.shouldBeLoginPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.DashboardPage.Companion.shouldBeDashboardPage
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.ZoneId

class UiInfrastructureFullStackTest(
    @param:Autowired private val jwtService: JwtService,
    @param:Autowired private val timeServiceSpy: TimeService,
) : SaFullStackTestBase() {

    @Test
    fun `should redirect to login page when auth expired`(page: Page) {
        // Mock server time to match the browser's mocked clock
        whenever(timeServiceSpy.currentTime()) doReturn TEST_FIXED_DATE_TIME
        whenever(timeServiceSpy.currentDate()) doReturn LocalDate.ofInstant(TEST_FIXED_DATE_TIME, ZoneId.of("UTC"))
        
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
