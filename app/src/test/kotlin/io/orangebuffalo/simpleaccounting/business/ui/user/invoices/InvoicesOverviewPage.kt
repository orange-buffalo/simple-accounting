package io.orangebuffalo.simpleaccounting.business.ui.user.invoices

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput.Companion.textInputByPlaceholder

class InvoicesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Invoices")
    val pageItems = components.overviewItems()
    val filterInput = components.textInputByPlaceholder("Search invoices")
    val createButton = components.buttonByText("Create new invoice")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeInvoicesOverviewPage(spec: InvoicesOverviewPage.() -> Unit = {}) {
            InvoicesOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openInvoicesOverviewPage(spec: InvoicesOverviewPage.() -> Unit = {}) {
            navigate("/invoices")
            shouldBeInvoicesOverviewPage(spec)
        }
    }
}
