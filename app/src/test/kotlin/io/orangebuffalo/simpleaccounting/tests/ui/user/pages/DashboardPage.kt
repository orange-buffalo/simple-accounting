package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DashboardCard
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DashboardCard.Companion.dashboardCardByIcon
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DashboardInvoiceCard
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DashboardInvoiceCard.Companion.dashboardInvoiceCardByIndex
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DateRangePicker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DateRangePicker.Companion.dateRangePicker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering

class DashboardPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Dashboard")
    private val dashboardContainer = page.locator(".sa-dashboard")
    private val invoiceCardsLocator = page.locator(".sa-dashboard__card:has(.sa-icon[data-icon=\"invoices-overview\"])")
    
    val expensesCard = components.dashboardCardByIcon("expense")
    val incomesCard = components.dashboardCardByIcon("income")
    val profitCard = components.dashboardCardByIcon("profit")
    val dateRangePicker = components.dateRangePicker()

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun shouldHaveInvoiceCards(count: Int) {
        invoiceCardsLocator.shouldHaveCount(count)
    }

    fun invoiceCard(index: Int, spec: DashboardInvoiceCard.() -> Unit) {
        components.dashboardInvoiceCardByIndex(index).spec()
    }

    fun reportRendering(name: String) {
        dashboardContainer.reportRendering(name)
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
