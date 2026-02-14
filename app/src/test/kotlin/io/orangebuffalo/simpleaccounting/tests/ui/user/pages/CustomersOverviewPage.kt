package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class CustomersOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Customers")
    val pageItems = components.overviewItems()
    val createButton = components.buttonByText("Add new")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCustomersOverviewPage(spec: CustomersOverviewPage.() -> Unit = {}) {
            CustomersOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCustomersOverviewPage(spec: CustomersOverviewPage.() -> Unit = {}) {
            navigate("/settings/customers")
            shouldBeCustomersOverviewPage(spec)
        }
    }
}
