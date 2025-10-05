package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker

class DashboardPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Dashboard")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        @UiComponentMarker
        fun Page.shouldBeDashboardPage(spec: DashboardPage.() -> Unit) {
            DashboardPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
