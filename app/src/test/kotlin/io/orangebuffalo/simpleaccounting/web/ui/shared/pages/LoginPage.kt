package io.orangebuffalo.simpleaccounting.web.ui.shared.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Checkbox.Companion.checkboxByOwnLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput.Companion.textInputByPlaceholder
import io.orangebuffalo.simpleaccounting.tests.infra.utils.navigateAndDisableAnimations

class LoginPage(page: Page) : SaPageBase<LoginPage>(page) {

    val loginInput = components.textInputByPlaceholder("Login")
    val passwordInput = components.textInputByPlaceholder("Password")
    val loginButton = components.buttonByText("Login")
    val rememberMeCheckbox = components.checkboxByOwnLabel("Remember me for 30 days")

    fun loginAs(user: PlatformUser) {
        loginInput.fill(user.userName)
        passwordInput.fill(user.passwordHash)
        loginButton.click()
    }

    fun shouldBeOpen(): LoginPage {
        loginInput.shouldBeVisible()
        passwordInput.shouldBeVisible()
        loginButton.shouldBeVisible()
        return this
    }
}

fun Page.openLoginPage(): LoginPage = LoginPage(navigateAndDisableAnimations("/"))

fun Page.loginAs(user: PlatformUser) = openLoginPage().loginAs(user)

fun Page.shouldBeLoginPage(): LoginPage = LoginPage(this).shouldBeOpen()
