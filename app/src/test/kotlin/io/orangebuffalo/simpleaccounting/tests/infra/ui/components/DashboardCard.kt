package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText

@UiComponentMarker
class DashboardCard private constructor(
    private val card: Locator,
) : UiComponent<DashboardCard>() {
    private val headerAmount = card.locator(".sa-dashboard__card__header__amount")
    private val headerFinalized = card.locator(".sa-dashboard__card__header__finalized")
    private val headerPending = card.locator(".sa-dashboard__card__header__pending")
    private val loader = card.locator(".sa-dashboard__card__header__loader")
    private val detailsItems = card.locator(".sa-dashboard__card__details__item")

    fun shouldBeLoading() {
        // Ensure the card itself is rendered before checking for the loader
        card.shouldBeVisible()
        loader.shouldBeVisible()
    }

    fun shouldBeLoaded() {
        headerAmount.shouldBeVisible()
    }

    /**
     * Ensures the card structure is rendered in the DOM.
     * This is useful in the initiator of withBlockedApiResponse to guarantee
     * the API request has been initiated before the route handler runs.
     */
    fun shouldBePresent() {
        card.shouldBeVisible()
    }

    fun shouldHaveAmount(expectedAmount: String) {
        headerAmount.shouldHaveText(expectedAmount)
    }

    fun shouldHaveFinalizedText(expectedText: String) {
        headerFinalized.shouldHaveText(expectedText)
    }

    fun shouldHavePendingText(expectedText: String) {
        headerPending.shouldHaveText(expectedText)
    }

    fun shouldHaveNoPendingText() {
        headerPending.shouldHaveText(" ")
    }

    fun shouldHaveDetailsItem(index: Int, expectedLabel: String, expectedValue: String) {
        detailsItems.nth(index).locator("span").first().shouldHaveText(expectedLabel)
        detailsItems.nth(index).locator("span").nth(1).shouldHaveText(expectedValue)
    }

    fun shouldHaveDetailsItemsCount(count: Int) {
        detailsItems.shouldHaveCount(count)
    }

    companion object {
        fun ComponentsAccessors.dashboardCardByIcon(icon: String) =
            DashboardCard(page.locator(".sa-dashboard__card:has(.sa-icon[data-icon=\"$icon\"])"))
    }
}
