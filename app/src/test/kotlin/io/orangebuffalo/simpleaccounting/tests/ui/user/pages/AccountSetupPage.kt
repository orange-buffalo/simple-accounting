package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker

class AccountSetupPage private constructor(page: Page) : SaPageBase(page) {
    val workspaceName = components.formItemTextInputByLabel("Workspace Name")
    val defaultCurrency = components.formItemTextInputByLabel("Main (default) Currency")
    val completeSetupButton = components.buttonByText("Complete setup")

    private fun shouldBeOpen() {
        workspaceName.shouldBeVisible()
        defaultCurrency.shouldBeVisible()
    }

    companion object {
        @UiComponentMarker
        fun Page.shouldBeAccountSetupPage(spec: AccountSetupPage.() -> Unit) {
            AccountSetupPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
