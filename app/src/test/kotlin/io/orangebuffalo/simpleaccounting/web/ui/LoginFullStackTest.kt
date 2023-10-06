package io.orangebuffalo.simpleaccounting.web.ui

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestData
import io.orangebuffalo.simpleaccounting.web.ui.pages.openLoginPage
import io.orangebuffalo.simpleaccounting.web.ui.pages.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.web.ui.pages.shouldBeUsersOverviewPage
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

    class LoginTestData : TestData {
        val fry = Prototypes.fry()
        val workspace = Prototypes.workspace(owner = fry)
        val farnsworth = Prototypes.farnsworth()
    }
}
