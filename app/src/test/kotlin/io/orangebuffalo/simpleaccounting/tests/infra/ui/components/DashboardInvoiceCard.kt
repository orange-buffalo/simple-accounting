package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
        fun ComponentsAccessors.dashboardInvoiceCardsData(): List<DashboardInvoiceCardData> {
            val json = page.locator(".sa-dashboard__card:has(.sa-icon[data-icon=\"invoices-overview\"])")
                .evaluateAll("""
                    cards => {
                        const text = element => element.textContent.replace(/\s+/g, ' ').trim();
                        return JSON.stringify(cards.map(card => ({
                            amount: text(card.querySelector('.sa-dashboard__card__header__amount')),
                            title: text(card.querySelector('.sa-dashboard__card__header__finalized:not(.sa-dashboard__invoice-status)')),
                            status: text(card.querySelector('.sa-dashboard__invoice-status')),
                            details: Array.from(card.querySelectorAll('.sa-dashboard__card__details__item')).map(item => ({
                                label: text(item.children[0]),
                                value: text(item.children[1])
                            }))
                        })));
                    }
                """) as String
            return Json.decodeFromString(json)
        }

        fun ComponentsAccessors.dashboardInvoiceCardByIndex(index: Int) =
            DashboardInvoiceCard(
                page.locator(".sa-dashboard__card:has(.sa-icon[data-icon=\"invoices-overview\"])").nth(index)
            )
    }
}

@Serializable
data class DashboardInvoiceCardData(
    val amount: String,
    val title: String,
    val status: String,
    val details: List<DashboardInvoiceDetailData>,
)

@Serializable
data class DashboardInvoiceDetailData(
    val label: String,
    val value: String,
)
