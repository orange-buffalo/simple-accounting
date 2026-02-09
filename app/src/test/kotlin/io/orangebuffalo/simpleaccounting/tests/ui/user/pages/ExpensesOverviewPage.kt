package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput.Companion.textInputByPlaceholder

class ExpensesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Expenses")
    val pageItems = components.overviewItems()
    val filterInput = components.textInputByPlaceholder("Search expenses")
    val createButton = components.buttonByText("Add new")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeExpensesOverviewPage(spec: ExpensesOverviewPage.() -> Unit = {}) {
            ExpensesOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openExpensesOverviewPage(spec: ExpensesOverviewPage.() -> Unit = {}) {
            navigate("/expenses")
            shouldBeExpensesOverviewPage(spec)
        }
    }
}
