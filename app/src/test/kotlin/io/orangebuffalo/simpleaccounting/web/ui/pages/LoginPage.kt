package io.orangebuffalo.simpleaccounting.web.ui.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class LoginPage(private val page: Page) {

    private val loginInput = page.getByPlaceholder("Login")
    private val passwordInput = page.getByPlaceholder("Password")
    private val loginButton = page.getByText("LOGIN")

    fun navigate() : LoginPage {
        page.navigate("/")
        return this
    }

    fun shouldHaveLoginButtonDisabled() : LoginPage {
        loginButton.assertThat().isDisabled()
        return this
    }

    fun shouldHaveLoginButtonEnabled() : LoginPage {
        loginButton.assertThat().isEnabled()
        return this
    }

    fun fillLogin(login: String) : LoginPage {
        loginInput.fill(login)
        return this
    }

    fun fillPassword(password: String) : LoginPage {
        passwordInput.fill(password)
        return this
    }

    fun clickLoginButton() : LoginPage {
        loginButton.click()
        return this
    }
}
