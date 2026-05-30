package io.orangebuffalo.simpleaccounting.business.ui.user.incomes

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewFilters.Companion.overviewFilters
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class IncomesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Incomes")
    val pageItems = components.overviewItems()
    val filters = components.overviewFilters()
    val createButton = components.buttonByText("Add new")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeIncomesOverviewPage(spec: IncomesOverviewPage.() -> Unit = {}) {
            IncomesOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openIncomesOverviewPage(spec: IncomesOverviewPage.() -> Unit = {}) {
            navigate("/incomes")
            shouldBeIncomesOverviewPage(spec)
        }
    }
}
