package io.orangebuffalo.simpleaccounting.business.ui.user.accountstatements

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewFilters.Companion.overviewFilters
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class AccountStatementsOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Account statements")
    val pageItems = components.overviewItems()
    val filters = components.overviewFilters()

    fun statement(title: String): SaOverviewItem {
        pageItems.shouldHaveDataSatisfying { items -> items.filter { it.title == title }.shouldHaveSize(1) }
        return pageItems.shouldHaveItemSatisfying { it.title == title }.also {
            it.shouldBeSingle()
        }
    }

    fun shouldHaveTypeBadge(title: String, status: String) {
        statement(title).middleColumnStatusLabel.shouldBeIconlessBadge(status)
    }

    companion object {
        fun Page.shouldBeAccountStatementsOverviewPage(spec: AccountStatementsOverviewPage.() -> Unit = {}) {
            AccountStatementsOverviewPage(this).apply {
                header.shouldBeVisible()
                spec()
            }
        }

        fun Page.openAccountStatementsOverviewPage(spec: AccountStatementsOverviewPage.() -> Unit = {}) {
            navigate("/account-statements")
            shouldBeAccountStatementsOverviewPage(spec)
        }
    }
}
