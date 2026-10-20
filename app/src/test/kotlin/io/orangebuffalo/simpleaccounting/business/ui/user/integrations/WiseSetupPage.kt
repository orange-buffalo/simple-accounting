package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveAttribute
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.WizardStep.Companion.wizardStep
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ComponentsAccessors
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Switch.Companion.switchByContainer

class WiseSetupPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Setup Wise integration")
    val token = components.formItemTextInputByLabel("Personal API token")
    val next = components.buttonByText("Next")
    val back = components.buttonByText("Back")
    val accounts = AccountCards(components)
    val status = components.statusLabel()
    val tokenStep = components.wizardStep("Personal token")
    val accountsStep = components.wizardStep("Select accounts")
    val configuration = Configuration(components)

    @UiComponentMarker
    class Configuration(components: ComponentsAccessors) {
        val fetchTransactions = components.switchByContainer(components.page.getByRole(AriaRole.REGION,
            Page.GetByRoleOptions().setName("Configuration").setExact(true)))
    }

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

    @UiComponentMarker
    class AccountCards(private val components: ComponentsAccessors) {
        private val container = components.page.locator(".integration-wizard__content")

        fun shouldHaveExactData(vararg expected: AccountData) {
            shouldSatisfy("Wise accounts should exactly match the expected data") {
                data().shouldContainExactly(*expected)
            }
        }

        private fun data(): List<AccountData> {
            val json = container.evaluate("""
                container => JSON.stringify(Array.from(container.querySelectorAll('.integration-card')).map(card => ({
                    profile: card.closest('section').querySelector('h2 span').textContent,
                    profileType: card.closest('section').querySelector('h2 small')?.textContent ?? null,
                    currency: card.querySelector('strong').textContent,
                    amount: card.querySelector('.integration-card__amount')?.textContent ?? null,
                    selected: card.getAttribute('aria-pressed') === 'true',
                    selectionLabel: Array.from(card.children).find(child => child.tagName === 'SPAN' && child.textContent.startsWith('✓'))?.textContent ?? null,
                    deleted: card.disabled
                })))
            """) as String
            return Json.decodeFromString(json)
        }

        fun account(currency: String): AccountCard {
            shouldSatisfy("Exactly one Wise account should have currency $currency") {
                data().filter { it.currency == currency }.shouldBeSingle()
            }
            val card = container.getByRole(AriaRole.BUTTON)
                .filter(Locator.FilterOptions().setHas(components.page.getByText(currency, Page.GetByTextOptions().setExact(true))))
            card.shouldHaveCount(1)
            return AccountCard(card)
        }
    }

    @Serializable
    data class AccountData(
        val profile: String,
        val profileType: String?,
        val currency: String,
        val amount: String?,
        val selected: Boolean = false,
        val selectionLabel: String? = null,
        val deleted: Boolean = false,
    )

    @UiComponentMarker
    class AccountCard(private val card: Locator) {
        fun shouldBeDeleted() = card.shouldBeDisabled()
        fun shouldBeUnselected() {
            card.shouldHaveAttribute(name = "aria-pressed", value = "false")
        }

        fun shouldBeSelected() {
            card.shouldHaveAttribute(name = "aria-pressed", value = "true")
        }

        fun click() = card.click()
    }

}
