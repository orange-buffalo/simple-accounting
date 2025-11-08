package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput.Companion.textInputByPlaceholder
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.innerTextOrNull

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
    val title: String?,
    val status: String?, // "success" or "pending"
    val statusText: String?, // "Finalized" or "Pending"
    val datePaid: String?,
    val amount: String?,
    val attributePreviewIcons: List<String>,
)

fun SaOverviewItem.toExpenseOverviewItem(): ExpenseOverviewItem {
    primaryAttributes.shouldHaveSize(1)

    // Determine status by checking the class on the status label in middle column
    val statusLabelLocator = this.locator(".overview-item__middle-column .sa-status-label")
    val statusClass = statusLabelLocator.getAttribute("class") ?: ""
    val status = when {
        statusClass.contains("sa-status-label_success") -> "success"
        statusClass.contains("sa-status-label_pending") -> "pending"
        else -> "unknown"
    }
    val statusText = statusLabelLocator.innerTextOrNull()

    return ExpenseOverviewItem(
        title = title.shouldNotBeNull(),
        status = status,
        statusText = statusText.shouldNotBeNull(),
        datePaid = primaryAttributes[0].text,
        amount = lastColumnContent.shouldNotBeNull(),
        attributePreviewIcons = attributePreviewIcons,
    )
}
