package io.orangebuffalo.simpleaccounting.web.ui.shared

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.web.ui.admin.pages.shouldBeUsersOverviewPage
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.openLoginPage
import io.orangebuffalo.simpleaccounting.web.ui.user.pages.shouldBeDashboardPage
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class LoginFullStackTest(
    preconditionsFactory: PreconditionsFactory,
) {

    @Test
    fun `should login as regular user`(page: Page) {
        page.openLoginPage()
            .loginButton { shouldBeDisabled() }
            .rememberMeCheckbox { shouldBeChecked() }
            .loginInput { fill(preconditions.fry.userName) }
            .loginButton { shouldBeDisabled() }
            .passwordInput { fill(preconditions.fry.passwordHash) }
            .loginButton {
                shouldBeEnabled()
                click()
            }
        page.shouldBeDashboardPage()
    }

    @Test
    fun `should login as admin user`(page: Page) {
        page.openLoginPage()
            .loginButton { shouldBeDisabled() }
            .rememberMeCheckbox { shouldBeChecked() }
            .loginInput { fill(preconditions.farnsworth.userName) }
            .loginButton { shouldBeDisabled() }
            .passwordInput { fill(preconditions.farnsworth.passwordHash) }
            .loginButton {
                shouldBeEnabled()
                click()
            }
        page.shouldBeUsersOverviewPage()
    }

    private val preconditions by preconditionsFactory {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val farnsworth = farnsworth()
        }
    }
}
