package io.orangebuffalo.simpleaccounting.web.ui.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser

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

fun Page.openLoginPage(): LoginPage {
    navigate("/")
    return LoginPage(this)
}

fun Page.loginAs(user: PlatformUser) = openLoginPage().loginAs(user)
