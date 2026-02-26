package io.orangebuffalo.simpleaccounting.business.ui.user.reporting

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class ReportingPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Reporting")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeReportingPage(spec: ReportingPage.() -> Unit = {}) {
            ReportingPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
