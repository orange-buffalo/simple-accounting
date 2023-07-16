package io.orangebuffalo.simpleaccounting.web.ui

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestData
import io.orangebuffalo.simpleaccounting.web.ui.pages.DashboardPage
import io.orangebuffalo.simpleaccounting.web.ui.pages.LoginPage
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class LoginFullStackTest {

    @Test
    fun `should login if credentials match`(page: Page, testData: LoginTestData) {
        LoginPage(page)
            .navigate()
            .shouldHaveLoginButtonDisabled()
            .fillLogin(testData.fry.userName)
            .shouldHaveLoginButtonDisabled()
            .fillPassword(testData.fry.passwordHash)
            .shouldHaveLoginButtonEnabled()
            .clickLoginButton()
        DashboardPage(page)
            .shouldBeOpen()
    }

    class LoginTestData : TestData {
        val fry = Prototypes.fry()
        val workspace = Prototypes.workspace(owner = fry)
    }
}
