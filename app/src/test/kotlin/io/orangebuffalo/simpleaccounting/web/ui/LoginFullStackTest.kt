package io.orangebuffalo.simpleaccounting.web.ui

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestData
import io.orangebuffalo.simpleaccounting.web.ui.pages.DashboardPage
import io.orangebuffalo.simpleaccounting.web.ui.pages.openLoginPage
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class LoginFullStackTest {

    @Test
    fun `should login if credentials match`(page: Page, testData: LoginTestData) {
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
        DashboardPage(page)
            .shouldBeOpen()
    }

    class LoginTestData : TestData {
        val fry = Prototypes.fry()
        val workspace = Prototypes.workspace(owner = fry)
    }
}
