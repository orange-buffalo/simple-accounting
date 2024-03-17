package io.orangebuffalo.simpleaccounting.web.ui.shared

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestDataDeprecated
import io.orangebuffalo.simpleaccounting.web.ui.admin.pages.shouldBeUsersOverviewPage
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.openLoginPage
import io.orangebuffalo.simpleaccounting.web.ui.user.pages.shouldBeDashboardPage
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class LoginFullStackTest {

    @Test
    fun `should login as regular user`(page: Page, testData: LoginTestData) {
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
    fun `should login as admin user`(page: Page, testData: LoginTestData) {
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

    class LoginTestData : TestDataDeprecated {
        val fry = Prototypes.fry()
        val workspace = Prototypes.workspace(owner = fry)
        val farnsworth = Prototypes.farnsworth()
    }
}
