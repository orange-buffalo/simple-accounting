package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput.Companion.textInputByPlaceholder
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering

class ExpensesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Expenses")
    private val expensesContainer = page.locator(".sa-page-content")
    val pageItems = components.overviewItems()
    val filterInput = components.textInputByPlaceholder("Search expenses")
    val createButton = components.buttonByText("Create new expense")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun reportRendering(name: String) {
        expensesContainer.reportRendering(name)
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

data class ExpenseOverviewItem(
    val title: String,
    val datePaid: SaOverviewItem.PrimaryAttribute,
    val hasNotesIcon: Boolean,
    val hasGeneralTaxIcon: Boolean,
    val hasAttachmentsIcon: Boolean,
    val hasForeignCurrencyIcon: Boolean,
    val hasPartialBusinessPurposeIcon: Boolean,
)

fun SaOverviewItem.toExpenseOverviewItem(): ExpenseOverviewItem {
    val panel = this
    primaryAttributes.shouldHaveSize(1)
    return ExpenseOverviewItem(
        title = panel.title.shouldNotBeNull(),
        datePaid = panel.primaryAttributes[0],
        hasNotesIcon = panel.hasAttributePreviewIcon("notes"),
        hasGeneralTaxIcon = panel.hasAttributePreviewIcon("tax"),
        hasAttachmentsIcon = panel.hasAttributePreviewIcon("attachment"),
        hasForeignCurrencyIcon = panel.hasAttributePreviewIcon("multi-currency"),
        hasPartialBusinessPurposeIcon = panel.hasAttributePreviewIcon("percent"),
    )
}
