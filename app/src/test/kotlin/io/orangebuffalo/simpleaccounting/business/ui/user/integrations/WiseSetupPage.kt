package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveAttribute
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
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

class WiseSetupPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Setup Wise integration")
    val token = components.formItemTextInputByLabel("Personal API token")
    val next = components.buttonByText("Next")
    val back = components.buttonByText("Back")
    val accounts = AccountCards(components)
    val status = components.statusLabel()
    val tokenStep = WizardStep(components, "Personal token")
    val accountsStep = WizardStep(components, "Select accounts")

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
        private val container = components.page.locator(".integration-cards")

        fun shouldHaveExactData(vararg expected: AccountData) {
            shouldSatisfy("Wise accounts should exactly match the expected data") {
                data().shouldContainExactly(*expected)
            }
        }

        private fun data(): List<AccountData> {
            val json = container.evaluate("""
                container => JSON.stringify(Array.from(container.querySelectorAll('button')).map(card => ({
                    profile: card.querySelector('strong').textContent,
                    currency: card.querySelectorAll('span')[0].textContent,
                    name: card.querySelectorAll('span')[1].textContent,
                    accountLabel: card.querySelector('small').textContent,
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
        val currency: String,
        val name: String,
        val accountLabel: String,
        val selected: Boolean = false,
        val selectionLabel: String? = null,
        val deleted: Boolean = false,
    )

    @UiComponentMarker
    class AccountCard(private val card: Locator) {
        fun shouldBeDeleted() = card.shouldBeDisabled()
        fun shouldBeUnselected() {
            card.shouldHaveAttribute("aria-pressed", "false")
        }

        fun shouldBeSelected() {
            card.shouldHaveAttribute("aria-pressed", "true")
        }

        fun click() = card.click()
    }

    @UiComponentMarker
    class WizardStep(components: ComponentsAccessors, title: String) {
        private val header = components.page.locator(".el-collapse-item__header")
            .filter(Locator.FilterOptions().setHasText(title))

        fun click() = header.click()
        fun shouldBeOpen() = header.shouldHaveAttribute("aria-expanded", "true")
        fun shouldBeCompleted() = header.locator(".step-number").shouldHaveAttribute("aria-label", "Completed")
        fun shouldBePending() = header.locator(".step-number").shouldHaveAttribute("aria-label", "Pending")
    }
}
