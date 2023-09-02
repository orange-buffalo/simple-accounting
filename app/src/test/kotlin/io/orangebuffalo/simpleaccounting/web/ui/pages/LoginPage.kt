package io.orangebuffalo.simpleaccounting.web.ui.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.ui.components.*

class LoginPage(page: Page) : SaPageBase<LoginPage>(page) {

    val loginInput = components.textInputByPlaceholder("Login")
    val passwordInput = components.textInputByPlaceholder("Password")
    val loginButton = components.buttonByText("LOGIN")
    val rememberMeCheckbox = components.checkboxByOwnLabel("Remember me for 30 days")
}

fun Page.openLoginPage(): LoginPage {
    navigate("/")
    return LoginPage(this)
}
