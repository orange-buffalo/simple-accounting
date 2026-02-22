package io.orangebuffalo.simpleaccounting.business.ui.shared.login

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByTestId
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Checkbox.Companion.checkboxByOwnLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput.Companion.textInputByPlaceholder
import java.util.regex.Pattern

class LoginPage private constructor(page: Page) : SaPageBase(page, ".login-page") {

    val loginInput = components.textInputByPlaceholder("Login")
    val passwordInput = components.textInputByPlaceholder("Password")
    val loginButton = components.buttonByTestId("login-button")
    val rememberMeCheckbox = components.checkboxByOwnLabel("Remember me for 30 days")
    private val errorMessage: Locator = page.locator(".login-page__login-error")

    val loginInputUk = components.textInputByPlaceholder("Логін")
    val passwordInputUk = components.textInputByPlaceholder("Пароль")

    fun loginAs(user: PlatformUser) {
        loginInput.fill(user.userName)
        passwordInput.fill(user.passwordHash)
        loginButton.click()
    }

    fun loginAsUk(user: PlatformUser) {
        loginInputUk.fill(user.userName)
        passwordInputUk.fill(user.passwordHash)
        loginButton.click()
    }

    private fun shouldBeOpen() {
        loginInput.shouldBeVisible()
        passwordInput.shouldBeVisible()
        loginButton.shouldBeVisible()
    }

    private fun shouldBeOpenUk() {
        loginInputUk.shouldBeVisible()
        passwordInputUk.shouldBeVisible()
        loginButton.shouldBeVisible()
    }

    fun shouldHaveErrorMessage(expectedMessage: String) {
        errorMessage.shouldHaveText(expectedMessage)
    }

    fun shouldHaveErrorMessageMatching(expectedPattern: String) {
        errorMessage.shouldHaveText(Pattern.compile(expectedPattern))
    }

    companion object {
        fun Page.openLoginPage(spec: LoginPage.() -> Unit) {
            navigate("/")
            LoginPage(this).spec()
        }

        fun Page.loginAs(user: PlatformUser) {
            openLoginPage { loginAs(user) }
        }

        fun Page.shouldBeLoginPage(spec: LoginPage.() -> Unit) {
            LoginPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.shouldBeLoginPageUk(spec: LoginPage.() -> Unit) {
            LoginPage(this).apply {
                shouldBeOpenUk()
                spec()
            }
        }
    }
}
