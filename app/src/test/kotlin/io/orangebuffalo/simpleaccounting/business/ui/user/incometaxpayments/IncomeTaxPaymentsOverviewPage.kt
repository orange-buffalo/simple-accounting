package io.orangebuffalo.simpleaccounting.business.ui.user.incometaxpayments

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class IncomeTaxPaymentsOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Income Tax Payments")
    val pageItems = components.overviewItems()
    val createButton = components.buttonByText("Add new")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeIncomeTaxPaymentsOverviewPage(spec: IncomeTaxPaymentsOverviewPage.() -> Unit = {}) {
            IncomeTaxPaymentsOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openIncomeTaxPaymentsOverviewPage(spec: IncomeTaxPaymentsOverviewPage.() -> Unit = {}) {
            navigate("/income-tax-payments")
            shouldBeIncomeTaxPaymentsOverviewPage(spec)
        }
    }
}
