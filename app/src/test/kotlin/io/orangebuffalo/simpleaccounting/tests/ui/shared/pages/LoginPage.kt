package io.orangebuffalo.simpleaccounting.tests.ui.shared.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Checkbox.Companion.checkboxByOwnLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput.Companion.textInputByPlaceholder
import io.orangebuffalo.simpleaccounting.tests.infra.utils.navigateAndDisableAnimations
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden

class LoginPage(page: Page) : SaPageBase<LoginPage>(page) {

    private val container = page.locator(".login-page")
    val loginInput = components.textInputByPlaceholder("Login")
    val passwordInput = components.textInputByPlaceholder("Password")
    val loginButton = components.buttonByText("Login")
    val rememberMeCheckbox = components.checkboxByOwnLabel("Remember me for 30 days")
    val errorMessage = page.locator(".login-page__login-error")

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
        errorMessage.waitFor(Locator.WaitForOptions().setTimeout(10000.0))
        errorMessage.shouldHaveText(expectedMessage)
        return this
    }

    fun shouldHaveNoErrorMessage(): LoginPage {
        errorMessage.shouldBeHidden()
        return this
    }

    fun reportRendering(name: String): LoginPage {
        container.reportRendering(name)
        return this
    }
}

fun Page.openLoginPage(): LoginPage = LoginPage(navigateAndDisableAnimations("/"))

fun Page.loginAs(user: PlatformUser) = openLoginPage().loginAs(user)

fun Page.shouldBeLoginPage(): LoginPage = LoginPage(this).shouldBeOpen()
