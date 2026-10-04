package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.WiseTokenInstructions.Companion.wiseTokenInstructions
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.IntegrationCards.Companion.integrationCards
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel

class WiseSetupPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Setup Wise integration")
    val token = components.formItemTextInputByLabel("Personal API token")
    val next = components.buttonByText("Next")
    val back = components.buttonByText("Back")
    val accounts = components.integrationCards()
    val status = components.statusLabel()
    val instructions = components.wiseTokenInstructions()

    companion object {
        fun Page.shouldBeWiseSetupPage(spec: WiseSetupPage.() -> Unit = {}) {
            WiseSetupPage(this).apply {
                header.shouldBeVisible()
                spec()
            }
        }

        fun Page.openWiseSetupPage(spec: WiseSetupPage.() -> Unit = {}) {
            navigate("/settings/integrations/wise/setup")
            shouldBeWiseSetupPage(spec)
        }

    }
}
