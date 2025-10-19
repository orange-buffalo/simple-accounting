package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker

class DashboardPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Dashboard")
    private val dashboardContainer = page.locator(".sa-dashboard")
    val expensesCard = DashboardCardAccessor(page, "expense")
    val incomesCard = DashboardCardAccessor(page, "income")
    val profitCard = DashboardCardAccessor(page, "profit")
    val dateRangePicker = page.locator(".el-date-editor")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun shouldHaveInvoiceCards(count: Int): DashboardPage {
        page.locator(".sa-dashboard__card:has(.sa-icon[data-icon=\"invoices-overview\"])").shouldHaveCount(count)
        return this
    }

    @UiComponentMarker
    class DashboardCardAccessor(private val page: Page, private val icon: String) {
        private val card = page.locator(".sa-dashboard__card:has(.sa-icon[data-icon=\"$icon\"])")
        private val headerAmount = card.locator(".sa-dashboard__card__header__amount")
        private val headerFinalized = card.locator(".sa-dashboard__card__header__finalized")
        private val headerPending = card.locator(".sa-dashboard__card__header__pending")
        private val loader = card.locator(".sa-dashboard__card__header__loader")
        private val detailsItems = card.locator(".sa-dashboard__card__details__item")

        fun shouldBeLoading(): DashboardCardAccessor {
            loader.shouldBeVisible()
            return this
        }

        fun shouldBeLoaded(): DashboardCardAccessor {
            // Card is loaded when the amount is visible
            headerAmount.shouldBeVisible()
            return this
        }

        fun shouldHaveAmount(expectedAmount: String): DashboardCardAccessor {
            headerAmount.shouldHaveText(expectedAmount)
            return this
        }

        fun shouldHaveFinalizedText(expectedText: String): DashboardCardAccessor {
            headerFinalized.shouldHaveText(expectedText)
            return this
        }

        fun shouldHavePendingText(expectedText: String): DashboardCardAccessor {
            headerPending.shouldHaveText(expectedText)
            return this
        }

        fun shouldHaveDetailsItem(index: Int, expectedLabel: String, expectedValue: String): DashboardCardAccessor {
            detailsItems.nth(index).locator("span").first().shouldHaveText(expectedLabel)
            detailsItems.nth(index).locator("span").nth(1).shouldHaveText(expectedValue)
            return this
        }

        fun shouldHaveDetailsItemsCount(count: Int): DashboardCardAccessor {
            detailsItems.shouldHaveCount(count)
            return this
        }
    }

    companion object {
        fun Page.shouldBeDashboardPage(spec: DashboardPage.() -> Unit = {}) {
            DashboardPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openDashboard(spec: DashboardPage.() -> Unit = {}) {
            navigate("/")
            shouldBeDashboardPage(spec)
        }
    }
}
