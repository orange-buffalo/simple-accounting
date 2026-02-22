package io.orangebuffalo.simpleaccounting.business.ui.user.dashboard

import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DashboardCard.Companion.dashboardCardByIcon
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DashboardInvoiceCard
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DashboardInvoiceCard.Companion.dashboardInvoiceCardByIndex
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DateRangePicker.Companion.dateRangePicker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class DashboardPage private constructor(page: Page, headerText: String = "Dashboard") : SaPageBase(page) {
    private val header = components.pageHeader(headerText)
    private val invoiceCardsLocator = page.locator(".sa-dashboard__card:has(.sa-icon[data-icon=\"invoices-overview\"])")

    val expensesCard = components.dashboardCardByIcon("expense")
    val incomesCard = components.dashboardCardByIcon("income")
    val profitCard = components.dashboardCardByIcon("profit")
    val dateRangePicker = components.dateRangePicker()

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun shouldBeLoaded() {
        expensesCard {
            shouldBeLoaded()
        }
        incomesCard {
            shouldBeLoaded()
        }
        profitCard {
            shouldBeLoaded()
        }
    }

    fun shouldHaveInvoiceCards(count: Int) {
        invoiceCardsLocator.shouldHaveCount(count)
    }

    fun invoiceCard(index: Int, spec: DashboardInvoiceCard.() -> Unit) {
        components.dashboardInvoiceCardByIndex(index).spec()
    }

    companion object {
        fun Page.shouldBeDashboardPage(spec: DashboardPage.() -> Unit = {}) {
            DashboardPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.shouldBeDashboardPageUk(spec: DashboardPage.() -> Unit = {}) {
            DashboardPage(this, "Кокпіт").apply {
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
