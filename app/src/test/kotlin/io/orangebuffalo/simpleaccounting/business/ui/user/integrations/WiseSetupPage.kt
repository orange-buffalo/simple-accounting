package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveAttribute
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel

class WiseSetupPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Setup Wise integration")
    val token = components.formItemTextInputByLabel("Personal API token")
    val next = components.buttonByText("Next")
    val back = components.buttonByText("Back")
    val accounts = AccountCards(page.locator(".integration-cards"))
    val status = components.statusLabel()

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

    class AccountCards(private val container: Locator) {
        fun account(profile: String, currency: String, name: String, id: String): AccountCard {
            val card = container.getByRole(AriaRole.BUTTON).filter(Locator.FilterOptions().setHasText("Account ID: $id"))
            return AccountCard(card, profile, currency, name, id)
        }
    }

    class AccountCard(
        private val card: Locator,
        private val profile: String,
        private val currency: String,
        private val name: String,
        private val id: String,
    ) {
        fun shouldBeUnselected() {
            card.shouldHaveAttribute("aria-pressed", "false")
            card.shouldHaveText("$profile$currency${name}Account ID: $id")
        }

        fun shouldBeSelected() {
            card.shouldHaveAttribute("aria-pressed", "true")
            card.shouldHaveText("$profile$currency${name}Account ID: $id✓ Selected")
        }

        fun click() = card.click()
    }
}
