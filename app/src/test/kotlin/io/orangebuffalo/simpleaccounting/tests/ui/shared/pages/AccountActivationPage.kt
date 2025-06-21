package io.orangebuffalo.simpleaccounting.tests.ui.shared.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ComponentsAccessors
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponent
import io.orangebuffalo.simpleaccounting.tests.infra.utils.navigateAndDisableAnimations

class AccountActivationPage(page: Page) : SaPageBase<AccountActivationPage>(page) {

    val userMessage = components.statusLabel()
    val form = AccountActivationForm(components)
    val loginButton = components.buttonByText("Login now")

    inner class AccountActivationForm(components: ComponentsAccessors<AccountActivationPage>) :
        UiComponent<AccountActivationPage, AccountActivationForm>(this) {
        val newPassword = components.formItemTextInputByLabel("New Password")
        val newPasswordConfirmation = components.formItemTextInputByLabel("New Password Confirmation")
        val activateAccountButton = components.buttonByText("Activate Account")

        fun shouldNotBeVisible() {
            newPassword.shouldBeHidden()
            newPasswordConfirmation.shouldBeHidden()
            activateAccountButton.shouldBeHidden()
        }

        fun shouldBeVisible() {
            newPassword.shouldBeVisible()
            newPasswordConfirmation.shouldBeVisible()
            activateAccountButton.shouldBeVisible()
        }
    }
}

fun Page.openAccountActivationPage(token: String?): AccountActivationPage =
    AccountActivationPage(navigateAndDisableAnimations("/activate-account${if (token != null) "/$token" else ""}"))
