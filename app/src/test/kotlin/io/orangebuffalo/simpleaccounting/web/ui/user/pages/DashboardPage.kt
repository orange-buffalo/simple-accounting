package io.orangebuffalo.simpleaccounting.web.ui.user.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class DashboardPage(page: Page) : SaPageBase<DashboardPage>(page) {
    private val header = components.pageHeader("Dashboard")

    fun shouldBeOpen() = header.shouldBeVisible()
}

fun Page.shouldBeDashboardPage(): DashboardPage = DashboardPage(this).shouldBeOpen()
