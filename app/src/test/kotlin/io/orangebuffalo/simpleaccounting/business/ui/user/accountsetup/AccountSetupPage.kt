package io.orangebuffalo.simpleaccounting.business.ui.user.accountsetup

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class AccountSetupPage private constructor(page: Page) : SaPageBase(page) {
    val workspaceName = components.formItemTextInputByLabel("Workspace Name")
    val defaultCurrency = components.formItemTextInputByLabel("Main (default) Currency")
    val completeSetupButton = components.buttonByText("Complete setup")

    private fun shouldBeOpen() {
        workspaceName.shouldBeVisible()
        defaultCurrency.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeAccountSetupPage(spec: AccountSetupPage.() -> Unit = {}) {
            AccountSetupPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
