package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DashboardCard
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DashboardCard.Companion.dashboardCardByIcon
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DateRangePicker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DateRangePicker.Companion.dateRangePicker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering

class DashboardPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Dashboard")
    private val dashboardContainer = page.locator(".sa-dashboard")
    private val expensesCardComponent = components.dashboardCardByIcon("expense")
    private val incomesCardComponent = components.dashboardCardByIcon("income")
    private val profitCardComponent = components.dashboardCardByIcon("profit")
    private val dateRangePickerComponent = components.dateRangePicker()
    private val invoiceCardsLocator = page.locator(".sa-dashboard__card:has(.sa-icon[data-icon=\"invoices-overview\"])")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun expensesCard(spec: DashboardCard.() -> Unit) {
        expensesCardComponent.spec()
    }

    fun incomesCard(spec: DashboardCard.() -> Unit) {
        incomesCardComponent.spec()
    }

    fun profitCard(spec: DashboardCard.() -> Unit) {
        profitCardComponent.spec()
    }

    fun dateRangePicker(spec: DateRangePicker.() -> Unit) {
        dateRangePickerComponent.spec()
    }

    fun shouldHaveInvoiceCards(count: Int) {
        invoiceCardsLocator.shouldHaveCount(count)
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
