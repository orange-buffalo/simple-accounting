package io.orangebuffalo.simpleaccounting.web.ui.shared

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.web.ui.admin.pages.shouldBeUsersOverviewPage
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.openLoginPage
import io.orangebuffalo.simpleaccounting.web.ui.user.pages.shouldBeDashboardPage
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SimpleAccountingFullStackTest
class LoginFullStackTest(
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @Test
    fun `should login as regular user`(page: Page) {
        val testData = setupPreconditions()

        page.openLoginPage()
            .loginButton { shouldBeDisabled() }
            .rememberMeCheckbox { shouldBeChecked() }
            .loginInput { fill(testData.fry.userName) }
            .loginButton { shouldBeDisabled() }
            .passwordInput { fill(testData.fry.passwordHash) }
            .loginButton {
                shouldBeEnabled()
                click()
            }
        page.shouldBeDashboardPage()
    }

    @Test
    fun `should login as admin user`(page: Page) {
        val testData = setupPreconditions()

        page.openLoginPage()
            .loginButton { shouldBeDisabled() }
            .rememberMeCheckbox { shouldBeChecked() }
            .loginInput { fill(testData.farnsworth.userName) }
            .loginButton { shouldBeDisabled() }
            .passwordInput { fill(testData.farnsworth.passwordHash) }
            .loginButton {
                shouldBeEnabled()
                click()
            }
        page.shouldBeUsersOverviewPage()
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
        val fry = fry()
        val workspace = workspace(owner = fry)
        val farnsworth = farnsworth()
    }
}
