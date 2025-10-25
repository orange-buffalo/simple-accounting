package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText

@UiComponentMarker
class DashboardInvoiceCard private constructor(
    private val card: Locator,
) : UiComponent<DashboardInvoiceCard>() {
    private val headerAmount = card.locator(".sa-dashboard__card__header__amount")
    private val headerTitle = card.locator(".sa-dashboard__card__header__finalized").first()
    private val headerStatus = card.locator(".sa-dashboard__card__header__finalized").nth(1)
    private val detailsItems = card.locator(".sa-dashboard__card__details__item")

    fun shouldHaveAmount(expectedAmount: String) {
        headerAmount.shouldHaveText(expectedAmount)
    }

    fun shouldHaveTitle(expectedTitle: String) {
        headerTitle.shouldHaveText(expectedTitle)
    }

    fun shouldHaveStatus(expectedStatus: String) {
        headerStatus.shouldHaveText(expectedStatus)
    }

    fun shouldHaveDetailsItem(index: Int, expectedLabel: String, expectedValue: String) {
        detailsItems.nth(index).locator("span").first().shouldHaveText(expectedLabel)
        detailsItems.nth(index).locator("span").nth(1).shouldHaveText(expectedValue)
    }

    companion object {
        fun ComponentsAccessors.dashboardInvoiceCardByIndex(index: Int) =
            DashboardInvoiceCard(
                page.locator(".sa-dashboard__card:has(.sa-icon[data-icon=\"invoices-overview\"])").nth(index)
            )
    }
}
