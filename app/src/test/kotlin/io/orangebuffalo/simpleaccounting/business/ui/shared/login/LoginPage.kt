package io.orangebuffalo.simpleaccounting.business.ui.shared.login

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByTestId
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Checkbox.Companion.checkboxByOwnLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput.Companion.textInputByPlaceholder
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import java.util.regex.Pattern

/**
 * Login is a two-step process: the username is submitted first, and the application then offers
 * the authentication methods available for that user.
 */
class LoginPage private constructor(page: Page) : SaPageBase(page, ".login-page") {

    val loginInput = components.textInputByPlaceholder("Login")
    val continueButton = components.buttonByTestId("continue-button")
    val passwordInput = components.textInputByPlaceholder("Password")
    val loginButton = components.buttonByTestId("login-button")
    val rememberMeCheckbox = components.checkboxByOwnLabel("Remember me for 30 days")
    val changeUserButton = components.buttonByTestId("change-user-button")
    private val errorMessage: Locator = page.locator(".login-page__login-error")

    val loginInputUk = components.textInputByPlaceholder("Логін")
    val passwordInputUk = components.textInputByPlaceholder("Пароль")

    fun oauthLoginButton(providerName: String) = components.buttonByText("Continue with $providerName")

    private val oauthLoginButtons: Locator = page.locator(".login-page__oauth-action")

    /**
     * The labels are rendered uppercase by the styles, so they are compared ignoring case.
     */
    fun shouldHaveOAuthProviders(vararg expectedLabels: String) {
        oauthLoginButtons.shouldSatisfy {
            allInnerTexts().map { it.trim().lowercase() }
                .shouldContainExactly(expectedLabels.map { it.lowercase() })
        }
    }

    fun submitUserName(userName: String) {
        loginInput.fill(userName)
        continueButton.click()
    }

    fun loginAs(user: PlatformUser) {
        submitUserName(user.userName)
        passwordInput.fill(user.passwordHash)
        loginButton.click()
    }

    fun loginAsUk(user: PlatformUser) {
        loginInputUk.fill(user.userName)
        continueButton.click()
        passwordInputUk.fill(user.passwordHash)
        loginButton.click()
    }

    private fun shouldBeOpen() {
        loginInput.shouldBeVisible()
        continueButton.shouldBeVisible()
    }

    private fun shouldBeOpenUk() {
        loginInputUk.shouldBeVisible()
        continueButton.shouldBeVisible()
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
