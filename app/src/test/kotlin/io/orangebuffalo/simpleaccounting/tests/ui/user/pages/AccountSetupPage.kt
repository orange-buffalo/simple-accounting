package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class AccountSetupPage(page: Page) : SaPageBase<AccountSetupPage>(page) {
    val workspaceName = components.formItemTextInputByLabel("Workspace Name")
    val defaultCurrency = components.formItemTextInputByLabel("Main (default) Currency")
    val completeSetupButton = components.buttonByText("Complete setup")

    fun shouldBeOpen(): AccountSetupPage {
        workspaceName.shouldBeVisible()
        defaultCurrency.shouldBeVisible()
        return this
    }
}

fun Page.shouldBeAccountSetupPage(): AccountSetupPage = AccountSetupPage(this).shouldBeOpen()

fun Page.shouldBeAccountSetupPage(spec: AccountSetupPage.() -> Unit) {
    shouldBeAccountSetupPage().spec()
}
