package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveAttribute
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText

class IntegrationCards private constructor(private val container: Locator) : UiComponent<IntegrationCards>() {
    fun shouldHaveWiseProvider() {
        container.getByRole(AriaRole.IMG, Locator.GetByRoleOptions().setName("Wise").setExact(true)).shouldBeVisible()
        container.locator("p").shouldHaveText("Connect your Wise currency balances and Jars to your workspace.")
    }

    fun setupWise() = container.getByRole(AriaRole.LINK, Locator.GetByRoleOptions().setName("Setup →").setExact(true)).click()
    fun viewWise() = container.getByRole(AriaRole.LINK, Locator.GetByRoleOptions().setName("View →").setExact(true)).click()

    fun account(profile: String, currency: String, name: String, id: String): WiseAccountCard {
        val card = container.getByRole(AriaRole.BUTTON).filter(
            Locator.FilterOptions().setHasText("Account ID: $id")
        )
        return WiseAccountCard(card, profile, currency, name, id)
    }

    companion object {
        fun ComponentsAccessors.integrationCards() = IntegrationCards(page.locator(".integration-cards"))
    }
}

class WiseAccountCard(
    private val card: Locator,
    private val profile: String,
    private val currency: String,
    private val name: String,
    private val id: String,
) : UiComponent<WiseAccountCard>() {
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
