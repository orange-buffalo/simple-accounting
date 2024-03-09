package io.orangebuffalo.simpleaccounting.web.ui.shared.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.infra.ui.components.Checkbox.Companion.checkboxByOwnLabel
import io.orangebuffalo.simpleaccounting.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.infra.ui.components.TextInput.Companion.textInputByPlaceholder
import io.orangebuffalo.simpleaccounting.infra.utils.openSimpleAccounting
import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser

class LoginPage(page: Page) : SaPageBase<LoginPage>(page) {

    val loginInput = components.textInputByPlaceholder("Login")
    val passwordInput = components.textInputByPlaceholder("Password")
    val loginButton = components.buttonByText("LOGIN")
    val rememberMeCheckbox = components.checkboxByOwnLabel("Remember me for 30 days")

    fun loginAs(user: PlatformUser) {
        loginInput.fill(user.userName)
        passwordInput.fill(user.passwordHash)
        loginButton.click()
    }
}

fun Page.openLoginPage(): LoginPage = LoginPage(openSimpleAccounting())

fun Page.loginAs(user: PlatformUser) = openLoginPage().loginAs(user)
