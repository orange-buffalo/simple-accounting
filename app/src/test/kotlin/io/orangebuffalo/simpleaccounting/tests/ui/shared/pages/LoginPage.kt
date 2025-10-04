package io.orangebuffalo.simpleaccounting.tests.ui.shared.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByTestId
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Checkbox.Companion.checkboxByOwnLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput.Companion.textInputByPlaceholder
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import java.util.regex.Pattern

class LoginPage(page: Page) : SaPageBase<LoginPage>(page) {

    private val container = page.locator(".login-page")
    val loginInput = components.textInputByPlaceholder("Login")
    val passwordInput = components.textInputByPlaceholder("Password")
    val loginButton = components.buttonByTestId("login-button")
    val rememberMeCheckbox = components.checkboxByOwnLabel("Remember me for 30 days")
    private val errorMessage: Locator = page.locator(".login-page__login-error")

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

    fun shouldHaveErrorMessage(expectedMessage: String): LoginPage {
        errorMessage.shouldHaveText(expectedMessage)
        return this
    }

    fun shouldHaveErrorMessageMatching(expectedPattern: String): LoginPage {
        errorMessage.shouldHaveText(Pattern.compile(expectedPattern))
        return this
    }

    fun reportRendering(name: String): LoginPage {
        container.reportRendering(name)
        return this
    }
}

fun Page.openLoginPage(): LoginPage = LoginPage(this.also {
    navigate("/")
})

fun Page.loginAs(user: PlatformUser) = openLoginPage().loginAs(user)

fun Page.shouldBeLoginPage(): LoginPage = LoginPage(this).shouldBeOpen()

fun Page.shouldBeLoginPage(spec: LoginPage.() -> Unit) {
    shouldBeLoginPage().spec()
}
